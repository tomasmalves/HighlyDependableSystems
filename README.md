# HighlyDependableSystems

Implementação de uma Blockchain para cadeira de Sistemas de Elevada Confiabilidade

## To test the project:

- Edit the `pom.xml` in the root directory:

  - In line 16, where it says:

    ```
       <local.jars.dir>/home/ubunto/Desktop/sec/project/HighlyDependableSystems/src/jars</local.jars.dir>
    ```

    Replace with the path of your jar folder:

    ```
       <local.jars.dir>/path/to/your/jars</local.jars.dir>
    ```

- Save and compile the project, in the root directory (HighlyDependableSystems/) using maven:

```
   mvn clean install -DskipTests
```

- Run ConsensusNode.java for N process implemented in the project. Replace N with the ConsensusNode Id. In our case, 4 processes starting in 1. Open a terminal window for each, and run:

```
   java -cp "target/classes:src/jars/*" consensus.ConsensusNode N
```

- Then, open a window for the client and run:

```
   java -cp "target/classes:src/jars/*" consensus.Client
```

- Send a message from the client and check the logs from the process terminal windows.

## Testing the contracts:

- Click _Run_ button on main class ISTCoinTest.java in folder `/src/test/java/contracts/ISTCoinTest.java` using your IDE. _No need to compile or run through the terminal_
