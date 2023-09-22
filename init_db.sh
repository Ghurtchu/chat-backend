#!/bin/bash

# Define PostgreSQL container name and credentials
CONTAINER_NAME="chat-postgres"
DB_USER="postgres"
DB_PASSWORD="mysecretpassword"
DB_NAME="mydatabase"

echo "Step 1: Check if the PostgreSQL container is running"
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "Error: The PostgreSQL container '$CONTAINER_NAME' is not running."
    exit 1
fi

echo "Step 2: Connect to PostgreSQL and create a database"
docker exec -it "$CONTAINER_NAME" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"

# sleep for 2 seconds just to ensure that database has been fully created
sleep 2;

echo "Step 3: Creating tables"
docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c "CREATE TABLE \"user\" (
 \"id\" SERIAL PRIMARY KEY,
 \"name\" VARCHAR(255) NOT NULL,
 \"email\" VARCHAR(255) NOT NULL
 );

 CREATE TABLE \"conversation\" (
     \"id\" SERIAL PRIMARY KEY,
     \"name\" VARCHAR(255) NOT NULL
 );

 CREATE TABLE \"message\" (
     \"id\" SERIAL PRIMARY KEY,
     \"text\" TEXT NOT NULL,
     \"conversationId\" SERIAL NOT NULL,
     \"fromUserId\" SERIAL NOT NULL,
     \"toUserId\" SERIAL NOT NULL,
     \"writtenAt\" TIMESTAMPTZ NOT NULL,
     FOREIGN KEY (\"conversationId\") REFERENCES \"conversation\"(\"id\"),
     FOREIGN KEY (\"fromUserId\") REFERENCES \"user\"(\"id\"),
     FOREIGN KEY (\"toUserId\") REFERENCES \"user\"(\"id\")
 );

 CREATE TABLE \"user_conversation\" (
     \"userId\" SERIAL NOT NULL,
     \"conversationId\" SERIAL NOT NULL,
     FOREIGN KEY (\"userId\") REFERENCES \"user\"(\"id\"),
     FOREIGN KEY (\"conversationId\") REFERENCES \"conversation\"(\"id\")
 );" <<EOF
EOF

if [ $? -eq 0 ]; then
    echo "Tables created successfully"
else
    echo "Error: Failed to write values to tables."
    exit 1
fi

echo "Step 4: Write initial values to the tables"
docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" <<EOF

-- Insert sample users into the "user" table
INSERT INTO "user" ("name", "email")
VALUES
    ('Alice', 'alice@example.com'),
    ('Bob', 'bob@example.com'),
    ('Charlie', 'charlie@example.com'),
    ('David', 'david@example.com'),
    ('Eve', 'eve@example.com'),
    ('Frank', 'frank@example.com');

-- Insert sample conversations into the "conversation" table
INSERT INTO "conversation" ("name")
VALUES
    ('General Chat'),
    ('Private Chat A'),
    ('Private Chat B');

-- Insert sample user-conversation associations into the "user_conversation" table
INSERT INTO "user_conversation" ("userId", "conversationId")
VALUES
    (1, 1),  -- User 1 is in Conversation 1
    (2, 1),  -- User 2 is in Conversation 1
    (3, 2),  -- User 3 is in Conversation 2
    (4, 2),  -- User 4 is in Conversation 2
    (5, 3),  -- User 5 is in Conversation 3
    (6, 3);  -- User 6 is in Conversation 3;

-- Sample messages for Conversation 1
INSERT INTO "message" ("text", "conversationId", "fromUserId", "toUserId", "writtenAt")
VALUES
    ('Hello there!', 1, 1, 2, '2023-09-22 10:00:00'),
    ('Hi! How are you?', 1, 2, 1, '2023-09-22 10:05:00'),
    ('Doing well, thanks!', 1, 1, 2, '2023-09-22 10:10:00'),
    ('What have you been up to?', 1, 1, 2, '2023-09-22 10:15:00'),
    ('Not much, just working.', 1, 2, 1, '2023-09-22 10:20:00'),
    ('That sounds busy!', 1, 1, 2, '2023-09-22 10:25:00'),
    ('Yeah, it is!', 1, 2, 1, '2023-09-22 10:30:00'),
    ('How is everyone?', 1, 1, 2, '2023-09-22 10:35:00'),
    ('Good, thanks!', 1, 2, 1, '2023-09-22 10:40:00'),
    ('Doing well here too!', 1, 3, 4, '2023-09-22 10:45:00');

-- Sample messages for Conversation 2
INSERT INTO "message" ("text", "conversationId", "fromUserId", "toUserId", "writtenAt")
VALUES
    ('This is a private message.', 2, 3, 4, '2023-09-22 11:00:00'),
    ('Another private message.', 2, 4, 3, '2023-09-22 11:05:00'),
    ('Let us discuss our project.', 2, 5, 6, '2023-09-22 11:10:00'),
    ('Sure, I have some ideas.', 2, 6, 5, '2023-09-22 11:15:00'),
    ('We can meet tomorrow.', 2, 3, 4, '2023-09-22 11:20:00'),
    ('That works for me.', 2, 4, 3, '2023-09-22 11:25:00'),
    ('How about 2 PM?', 2, 5, 6, '2023-09-22 11:30:00'),
    ('Sounds good!', 2, 6, 5, '2023-09-22 11:35:00'),
    ('Greetings from Conversation 2.', 2, 5, 6, '2023-09-22 11:40:00'),
    ('Hello there again!', 2, 6, 5, '2023-09-22 11:45:00');

-- Sample messages for Conversation 3
INSERT INTO "message" ("text", "conversationId", "fromUserId", "toUserId", "writtenAt")
VALUES
    ('Greetings from Conversation 3.', 3, 5, 6, '2023-09-22 12:00:00'),
    ('Hello there!', 3, 6, 5, '2023-09-22 12:05:00'),
    ('How is it going?', 3, 5, 6, '2023-09-22 12:10:00'),
    ('Doing well!', 3, 6, 5, '2023-09-22 12:15:00'),
    ('What is new?', 3, 5, 6, '2023-09-22 12:20:00'),
    ('Not much, just enjoying the day.', 3, 6, 5, '2023-09-22 12:25:00'),
    ('How about a coffee later?', 3, 5, 6, '2023-09-22 12:30:00'),
    ('Sure, I would love that!', 3, 6, 5, '2023-09-22 12:35:00'),
    ('Meet you at the usual place?', 3, 5, 6, '2023-09-22 12:40:00'),
    ('Sounds like a plan!', 3, 6, 5, '2023-09-22 12:45:00');
EOF

if [ $? -eq 0 ]; then
    echo "Values written successfully."
else
    echo "Error: Failed to write values."
    exit 1
fi
