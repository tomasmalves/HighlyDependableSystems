# HighlyDependableSystems

Implementação de uma Blockchain para cadeira de Sistemas de Elevada Confiabilidade

## To test the project:

- Save and compile the project, in the root directory (HighlyDependableSystems/) using maven:

```
   mvn clean install
```

- Run ConsensusNode.java for N process implemented in the project. Replace N with the ConsensusNode Id. In our case, 4 processes starting in 1. Open a terminal window for each, and run:

```
   java -cp "target/classes:src/main/resources/jars/*" consensus.ConsensusNode N
```

- Then, open a window for the client and run:

```
   java -cp "target/classes:src/main/resources/jars/*" consensus.Client
```

- Send a message from the client and check the logs from the process terminal windows.

## Testing the contracts:

- Click _Run_ button on main class ISTCoinTest.java in folder `/src/test/java/contracts/ISTCoinTest.java` using your IDE. _No need to compile or run through the terminal_
