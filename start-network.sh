#!/bin/bash

# Set the project directory
PROJECT_DIR="."
MAIN_CLASS="src/DepChainApplication.java"

# Compile the project if needed
echo "Compiling project..."
javac -d $PROJECT_DIR/class $PROJECT_DIR/src/**/*.java

# Check if compilation was successful
if [ $? -ne 0 ]; then
    echo "Compilation failed. Exiting."
    exit 1
fi

# Create a log directory if it doesn't exist
mkdir -p logs

# Function to start a node in a new terminal
start_node() {
    local NODE_ID=$1
    local NODE_TYPE=$2
    
    # Determine the command to launch a new terminal based on available terminal emulators
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "echo 'Starting $NODE_TYPE node (ID: $NODE_ID)...'; java -cp $PROJECT_DIR/class $MAIN_CLASS $NODE_ID; exec bash"
    elif command -v xterm &> /dev/null; then
        xterm -title "$NODE_TYPE Node $NODE_ID" -e "java -cp $PROJECT_DIR/class $MAIN_CLASS $NODE_ID; exec bash" &
    elif command -v konsole &> /dev/null; then
        konsole --new-tab -p tabtitle="$NODE_TYPE Node $NODE_ID" -e "bash -c 'java -cp $PROJECT_DIR/class $MAIN_CLASS $NODE_ID; exec bash'" &
    elif command -v terminator &> /dev/null; then
        terminator -e "bash -c 'java -cp $PROJECT_DIR/class $MAIN_CLASS $NODE_ID; exec bash'" --title="$NODE_TYPE Node $NODE_ID" &
    else
        echo "No supported terminal emulator found. Please install gnome-terminal, xterm, konsole, or terminator."
        exit 1
    fi
    
    # Return the PID of the terminal process
    echo $!
}

# Start the leader (node 0)
echo "Starting leader node (ID: 0)..."
LEADER_PID=$(start_node 0 "Leader")

# Wait for the leader to initialize
sleep 2

# Start follower nodes
FOLLOWER_PIDS=""
for ID in 1 2 3; do
    echo "Starting follower node (ID: $ID)..."
    PID=$(start_node $ID "Follower")
    FOLLOWER_PIDS="$FOLLOWER_PIDS $PID"
    
    # Short delay between starting nodes
    sleep 1
done

echo "All nodes started in separate terminals."
echo "Leader terminal PID: $LEADER_PID"
echo "Follower terminal PIDs: $FOLLOWER_PIDS"
echo "Close the terminals when you're done."
echo "Press Ctrl+C to terminate this script..."

# Wait for Ctrl+C
trap "echo 'Script terminated.'; exit 0" INT
wait