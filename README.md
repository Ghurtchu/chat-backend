An attempt to implement the persistent chat server with the help of PostgreSQL backend.

For now a single user will have two websocket connections to the backend:
- background WS connection (`ws://localhost:8080/conversations/{userId}`):
  - expects the number from the frontend, let's say: `10` and publishes `Msg.LoadConversations(10)` in a `Topic`
  - then it loads last `10` conversations only (sorted by message timestamps), can load more if the user scrolls down (pagination)
  - tracks if new messages were sent from other users while he's chatting with someone else (kinda like notification tracker) and if so, updates the loaded conversations in the background, so that UI has fresh data
- primary WS connection (`ws://localhost:8080/from/{fromUserId}/to/{toUserId}/conversation/{conversationId}`):
  - main chat ws connection which sends and expects chat messages from and to user
  - upon receiving `Msg.ChatMessage` it inserts the new record in `DB` and publishes the message in `Topic` so that background WS connection updates the conversations UI

Not much done yet, still in progress :D
