# HighlyDependableSystems

Implementação de uma Blockchain para cadeira de Sistemas de Elevada Confiabilidade

## To test the project:

- Compile the project, in the root directory of your project (HighlyDependableSystems/):

```
   javac -d class $(find src -name "*.java")
```

- Run ConsensusNode.java for N process implemented in the project. Replace N with the ConsensusNode Id. In our case, 4 processes starting in 1. Open a terminal window for each, and run:

```
   java -cp class consensus.ConsensusNode N
```

- Then, open a window for the client and run:

```
   java -cp class consensus.Client
```

- Send a message from the client and check the logs from the process terminal windows.
