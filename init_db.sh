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

echo "Step 2: Connect to PostgreSQL and create a table"
docker exec -it "$CONTAINER_NAME" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"

sleep 2;

docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c "CREATE TABLE IF NOT EXISTS mytable (
    id SERIAL PRIMARY KEY,
    column1 VARCHAR(255),
    column2 VARCHAR(255)
);"

echo "Table 'mytable' created successfully."

echo "Step 3: Write values to the table"
docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" <<EOF
INSERT INTO mytable (column1, column2) VALUES ('value1', 'value2');
INSERT INTO mytable (column1, column2) VALUES ('value3', 'value4');
-- Add more INSERT statements as needed
EOF

if [ $? -eq 0 ]; then
    echo "Values written to 'mytable' successfully."
else
    echo "Error: Failed to write values to 'mytable'."
    exit 1
fi
