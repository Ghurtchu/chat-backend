
An attempt to implement the persistent chat server with the help of PostgreSQL backend.

For now a single user will have two websocket connections to the backend:
- background WS connection (`ws://localhost:8080/conversations/{userId}`):
  - expects the number from the frontend, let's say: `10` and publishes `Msg.LoadConversations(10)` in a `Topic`
  - then it loads last `10` conversations only (sorted by message timestamps), can load more if the user scrolls down (pagination)
  - tracks if new messages were sent from other users while he's chatting with someone else (kinda like notification tracker) and if so, updates the loaded conversations in the background, so that UI has fresh data
- primary WS connection (`ws://localhost:8080/from/{fromUserId}/to/{toUserId}/conversation/{conversationId}`):
  - main chat ws connection which sends and expects chat messages from and to user
  - upon receiving `Msg.ChatMessage` it inserts the new record in `DB` and publishes the message in `Topic` so that background WS connection updates the conversations UI

P.S user auth & login will be implemented based on email & password and JWT as a separate backend in Java by my friend (using shared PostgreSQL database)

Setup instructions:

## Step 1: Clone the Repository
Clone the GitHub repository

```bash
git clone https://github.com/Ghurtchu/msgFlow.git
cd msgFlow
```

## Step 2: Start PostgreSQL with Docker
```bash
docker run --name chat-postgres -e POSTGRES_PASSWORD=mysecretpassword -d -p 5433:5432 postgres  
```

## Step 3: Verify PostgreSQL Container is Running
```bash
docker ps -a
```
You should see an entry for the chat-postgres container in the list.

## Step 4: Initialize DB in docker container
```bash
./init_db.sh
```
If you can't execute this runnable, give your user the permission (depends which shell are you using)

## Step 5: Start server
```bash
sbt run
```

## Step 6: Use your favorite WS tool for testing the functionality
```bash
# in progress :)
```