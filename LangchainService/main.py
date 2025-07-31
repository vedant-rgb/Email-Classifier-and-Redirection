from fastapi import FastAPI
from pydantic import BaseModel, Field
from fastapi.responses import JSONResponse
from langchain.chains import RetrievalQA
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain.docstore.document import Document
from dotenv import load_dotenv
import os
import json
import re
from bs4 import BeautifulSoup

# Load environment variables
load_dotenv()

# Initialize FastAPI app
app = FastAPI()

# Function to clean email body
def clean_email_body(body: str) -> str:
    """
    Clean email body by removing forwarded message headers and HTML tags.
    """
    cleaned = re.sub(r'-{10}\s*Forwarded message\s*-{10}.*?(?=(Hi|Dear|$))', '', body, flags=re.DOTALL)
    match = re.search(r'(Hi|Dear).*', cleaned, re.DOTALL)
    if match:
        cleaned = match.group(0)
    cleaned = BeautifulSoup(cleaned, "html.parser").get_text()
    cleaned = re.sub(r'\n\s*\n', '\n', cleaned.strip())
    return cleaned

# Load and prepare RAG documents from JSON
def load_json_and_convert_to_text(json_path):
    """
    Load company structure from JSON and convert to text for document creation, including descriptions and keywords.
    """
    with open(json_path, 'r') as f:
        data = json.load(f)
    
    text_content = []
    company_name = data.get("company_name", "Unknown Company")
    company_desc = data.get("description", "No company description provided")
    text_content.append(f"Company: {company_name}\nDescription: {company_desc}")
    
    for dept in data.get("departments", []):
        dept_name = dept.get("name", "Unknown Department")
        dept_desc = dept.get("description", "No department description provided")
        text_content.append(f"\nDepartment: {dept_name}\nDescription: {dept_desc}")
        for team in dept.get("teams", []):
            team_name = team.get("name", "Unknown Team")
            team_desc = team.get("description", "No team description provided")
            text_content.append(f"  Team: {team_name}\n  Description: {team_desc}")
            for employee in team.get("employees", []):
                name = employee.get("name", "Unknown Employee")
                email = employee.get("email", "unknown@yourcompany.com")
                responsibility = employee.get("responsibility", "No responsibility specified")
                keywords = ", ".join(employee.get("keywords", []))
                text_content.append(
                    f"    Employee: {name}\n    Email: {email}\n    Responsibility: {responsibility}\n    Keywords: {keywords}"
                )
    
    return "\n".join(text_content)

# Load JSON and create Document objects
json_path = "data/company_structure.json"
text_content = load_json_and_convert_to_text(json_path)
docs = [Document(page_content=text_content)]

# Split documents into chunks
splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,  # Suitable for enriched content
    chunk_overlap=100,
    separators=["\n\n", "\n", ".", " ", ""]
)
split_docs = splitter.split_documents(docs)

# Print all chunks at startup
print("\n📚 All FAISS Chunks:")
for i, doc in enumerate(split_docs):
    print(f"Chunk {i+1}:")
    print(f"{doc.page_content[:500]}...")
    print("-" * 80)

# Create embeddings and vector store
embedding = OpenAIEmbeddings(model="text-embedding-3-small")
vectordb = FAISS.from_documents(split_docs, embedding)

# Configure retriever
retriever = vectordb.as_retriever(
    search_type="similarity_score_threshold",
    search_kwargs={"k": 2, "score_threshold": 0.3}
)

# Initialize LLM
llm = ChatOpenAI(
    model="gpt-3.5-turbo",
    temperature=0,
    seed=42,
    max_tokens=150
)

# Create QA chain
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=retriever,
    return_source_documents=True
)

# DTO for incoming email
class EmailMessageDTO(BaseModel):
    subject: str
    from_: str = Field(..., alias="from")
    body: str

    class Config:
        validate_by_name = True

# Main analysis endpoint
@app.post("/analyze_email/")
async def analyze_email(email: EmailMessageDTO):
    """
    Analyze an email and determine sentiment and forwarding email based on company structure.
    """
    # Clean email body
    cleaned_body = clean_email_body(email.body)
    print(cleaned_body)
    # Load full company structure for prompt
    company_structure = load_json_and_convert_to_text(json_path)
    # Construct prompt with full company structure
    prompt = f"""You are an email routing assistant. Analyze the email below and respond with ONLY a valid JSON object. Do not include any additional text, explanations, or formatting like ```json. Select an employee email from the provided company structure; do not use email addresses from the email's From field unless they match an employee in the structure.

Company Structure:
{company_structure}

Email:
Subject: {email.subject}
From: {email.from_}
Body: {cleaned_body}

Based on the company structure, determine:
1. Sentiment: "Positive", "Negative", or "Neutral"
2. Best employee email to forward to, or "not_found" if no match

Response format:
{{"sentiment": "string", "forward_to": "string"}}"""

    try:
        # Debug: Log similarity search results with scores
        search_results = vectordb.similarity_search_with_score(prompt, k=2, score_threshold=0.2)
        print("\n📚 Similarity Search Results:")
        if not search_results:
            print("No chunks retrieved above score threshold 0.2")
        for doc, score in search_results:
            print(f"Score: {score:.4f}, Content: {doc.page_content[:200]}...")

        # Invoke QA chain
        result = qa_chain.invoke({"query": prompt})
        raw_response = result["result"].strip()
        
        print("🧠 Raw LLM Output:", raw_response)
        
        # Extract JSON from response
        json_match = re.search(r'\{.*\}', raw_response, re.DOTALL)
        if json_match:
            json_str = json_match.group()
            parsed_result = json.loads(json_str)
            
            # Validate required fields
            if "sentiment" not in parsed_result or "forward_to" not in parsed_result:
                raise ValueError("Missing required fields")
                
            # Validate sentiment values
            valid_sentiments = ["Positive", "Negative", "Neutral"]
            if parsed_result["sentiment"] not in valid_sentiments:
                parsed_result["sentiment"] = "Neutral"
                
        else:
            raise ValueError("No valid JSON found in response")
            
        # Log retrieved context
        print("\n📚 Retrieved Context:")
        if "source_documents" in result:
            retrieved_docs = result["source_documents"]
            if not retrieved_docs:
                print("No source documents retrieved")
            for i, doc in enumerate(retrieved_docs):
                print(f"Chunk {i+1}: {doc.page_content[:200]}...")
        else:
            print("No source documents available")
        
        return {"analysis": parsed_result}
        
    except Exception as e:
        print(f"❌ Error: {e}")
        return {"analysis": {"sentiment": "Neutral", "forward_to": "not_found", "error": str(e)}}

# Endpoint to view FAISS chunks
@app.get("/view_chunks")
async def view_chunks():
    """
    Return all chunks stored in the FAISS vector store for debugging.
    """
    texts = [doc.page_content for doc in split_docs]
    return JSONResponse(content={
        "total_chunks": len(texts),
        "chunks": texts
    })