package com.vedant.email_fetcher_service.services;

import com.vedant.email_fetcher_service.dto.Attachment;
import com.vedant.email_fetcher_service.dto.EmailMessageDTO;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

@Service
public class InboxReader {
    private static final Logger logger = LoggerFactory.getLogger(InboxReader.class);

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${kafka.topic.email}")
    private String emailTopic;

    private final KafkaTemplate<String, EmailMessageDTO> kafkaTemplate;
    private Properties mailProperties;

    public InboxReader(KafkaTemplate<String, EmailMessageDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing IMAP properties for host: {}", host);
        this.mailProperties = new Properties();
        mailProperties.put("mail.store.protocol", "imap");
        mailProperties.put("mail.imap.ssl.enable", "true");
        mailProperties.put("mail.imap.host", host);
        mailProperties.put("mail.imap.port", "993");
    }

    public void fetchEmails() throws MessagingException, IOException {
        Session emailSession = Session.getDefaultInstance(mailProperties);
        Store store = emailSession.getStore("imap");
        store.connect(host, username, password);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        for (Message message : messages) {
            EmailMessageDTO emailMessage = new EmailMessageDTO();
            emailMessage.setSubject(message.getSubject());
            emailMessage.setFrom(message.getFrom()[0].toString());

            logger.info("Processing email: {}", message.getSubject());
            processMessage(message, emailMessage);

            // Send to Kafka
            try {
                kafkaTemplate.send(emailTopic, emailMessage.getSubject(), emailMessage);
                logger.info("Sent email to Kafka topic: {}", emailMessage.getSubject());
            } catch (Exception e) {
                logger.error("Failed to send email to Kafka: {}", emailMessage.getSubject(), e);
            }

            // Mark email as read
            try {
                message.setFlag(Flags.Flag.SEEN, true);
                logger.info("Marked email as read: {}", message.getSubject());
            } catch (MessagingException e) {
                logger.error("Failed to mark email as read: {}", message.getSubject(), e);
            }
        }

        inbox.close(false);
        store.close();
    }

    private void processMessage(Message message, EmailMessageDTO emailMessage) throws MessagingException, IOException {
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                processBodyPart(bodyPart, emailMessage);
            }
        } else if (message.isMimeType("text/plain")) {
            emailMessage.setBody((String) message.getContent());
        } else if (message.isMimeType("text/html")) {
            emailMessage.setBody((String) message.getContent());
        }
    }

    private void processBodyPart(BodyPart bodyPart, EmailMessageDTO emailMessage) throws MessagingException, IOException {
        String disposition = bodyPart.getDisposition();
        if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
            processAttachment(bodyPart, emailMessage);
        } else if (bodyPart.isMimeType("text/plain")) {
            emailMessage.setBody((String) bodyPart.getContent());
        } else if (bodyPart.isMimeType("text/html")) {
            emailMessage.setBody((String) bodyPart.getContent());
        } else if (bodyPart.isMimeType("multipart/*")) {
            Multipart nestedMultipart = (Multipart) bodyPart.getContent();
            for (int i = 0; i < nestedMultipart.getCount(); i++) {
                processBodyPart(nestedMultipart.getBodyPart(i), emailMessage);
            }
        }
    }

    private void processAttachment(BodyPart bodyPart, EmailMessageDTO emailMessage) throws MessagingException, IOException {
        String fileName = bodyPart.getFileName();
        if (fileName != null) {
            fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String mimeType = bodyPart.getContentType().split(";")[0];
            byte[] content = ((MimeBodyPart) bodyPart).getInputStream().readAllBytes();
            String base64Content = Base64.getEncoder().encodeToString(content);

            Attachment attachment = new Attachment(fileName, mimeType, base64Content);
            emailMessage.addAttachment(attachment);
            logger.info("Added attachment to message: {}", fileName);
        }
    }
}