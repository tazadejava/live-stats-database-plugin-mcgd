package me.tazadejava.main;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Handles logging the logs on the server onto a MongoDB database, which in turn is read to display on the frontend website.
 */
public class LogAppender extends AbstractAppender {

    private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    private MongoCollection<Document> collection;

    public LogAppender(String databaseID, String groupNumber) {
        super("LiveStatsLogger", null, null);
        start();

        connectToDatabase(databaseID, groupNumber);
    }

    private void connectToDatabase(String databaseID, String groupNumber) {
        MongoClient client = MongoClients.create(databaseID);

        MongoDatabase db = client.getDatabase("mcgd");

        String collectionName = "logGroup" + groupNumber;
        collection = db.getCollection(collectionName);

        collection.dropIndex(Indexes.ascending("createdAt"));
        collection.createIndex(Indexes.ascending("createdAt"), new IndexOptions().expireAfter(86400L, TimeUnit.SECONDS));
    }

    //strips the message of ugly chatcolor remnants
    private String strip(String str) {
        StringBuilder newStr = new StringBuilder();

        boolean isInIllegalChar = false;
        char[] arr = str.toCharArray();
        for(int i = 0; i < arr.length; i++) {
            if(isInIllegalChar) {
                if(arr[i] == 'm') {
                    isInIllegalChar = false;
                }
            } else {
                if (arr[i] == '\u001B') {
                    isInIllegalChar = true;
                } else {
                    newStr.append(arr[i]);
                }
            }
        }

        return newStr.toString();
    }

    @Override
    public void append(LogEvent logEvent) {
        LogEvent log = logEvent.toImmutable();
        String message = log.getMessage().getFormattedMessage();

        //don't log IP addresses
        if(message.contains("] logged in with entity id ")) {
            return;
        }

        //don't log IP addresses
        if(message.contains("Disconnecting com.mojang.authlib.GameProfile")) {
            return;
        }

        message = strip(message);

        logMessage(logEvent, message);

        if(logEvent.getThrown() != null) {
            Throwable thrown = logEvent.getThrown();
            logMessage(logEvent, thrown.getMessage());

            for(StackTraceElement element : thrown.getStackTrace()) {
                logMessage(logEvent, element.toString());
            }
        }
    }

    private void logMessage(LogEvent logEvent, String message) {
        String timedMessage = "[" + formatter.format(new Date(logEvent.getTimeMillis())) + "] " + message;

        Document doc = new Document("content", timedMessage);
        doc.append("createdAt", new Date());
        collection.insertOne(doc);
    }
}
