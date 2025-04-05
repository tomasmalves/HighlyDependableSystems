package contracts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;

import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

public class ISTCoinTest {

    public static void main(String[] args) {
        SimpleWorld simpleWorld = new SimpleWorld();

        // DEPCOIN

        // Set up the owner account
        Address ownerAddress = Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
        simpleWorld.createAccount(ownerAddress, 0, Wei.fromEth(100000));
        MutableAccount ownerAccount = (MutableAccount) simpleWorld.get(ownerAddress);
        System.out.println("Owner Account");
        System.out.println("  Address: " + ownerAccount.getAddress());
        System.out.println("  Balance: " + ownerAccount.getBalance());
        System.out.println("  Nonce: " + ownerAccount.getNonce());
        System.out.println();

        // Set up user account (for transfer testing)
        Address userAddress = Address.fromHexString("1234567891234567891234567891234567891234");
        simpleWorld.createAccount(userAddress, 0, Wei.fromEth(0));
        MutableAccount userAccount = (MutableAccount) simpleWorld.get(userAddress);
        System.out.println("User Account");
        System.out.println("  Address: " + userAccount.getAddress());
        System.out.println("  Balance: " + userAccount.getBalance());
        System.out.println("  Nonce: " + userAccount.getNonce());
        System.out.println();

        String istCoinDeploymentBytecode = "6080604052600260075f6101000a81548160ff021916908360ff16021790555060075f9054906101000a900460ff1660ff16600a61003d91906107bd565b6305f5e10061004c9190610807565b60085534801561005a575f80fd5b50336040518060400160405280600881526020017f49535420436f696e0000000000000000000000000000000000000000000000008152506040518060400160405280600381526020017f495354000000000000000000000000000000000000000000000000000000000081525081600390816100d79190610a79565b5080600490816100e79190610a79565b5050505f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff160361015a575f6040517f1e4fbdf70000000000000000000000000000000000000000000000000000000081526004016101519190610b87565b60405180910390fd5b6101698161018160201b60201c565b5061017c3360085461024460201b60201c565b610d36565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508160055f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036102b4575f6040517fec442f050000000000000000000000000000000000000000000000000000000081526004016102ab9190610b87565b60405180910390fd5b6102c55f83836102c960201b60201c565b5050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1614158015610310575061030f836103ed60201b60201c565b5b15610350576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161034790610bfa565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16141580156103975750610396826103ed60201b60201c565b5b156103d7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103ce90610c88565b60405180910390fd5b6103e883838361043f60201b60201c565b505050565b5f60065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff160361048f578060025f8282546104839190610ca6565b9250508190555061055d565b5f805f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905081811015610518578381836040517fe450d38c00000000000000000000000000000000000000000000000000000000815260040161050f93929190610ce8565b60405180910390fd5b8181035f808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550505b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036105a4578060025f82825403925050819055506105ee565b805f808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f82825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405161064b9190610d1d565b60405180910390a3505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f8160011c9050919050565b5f808291508390505b60018511156106da578086048111156106b6576106b5610658565b5b60018516156106c55780820291505b80810290506106d385610685565b945061069a565b94509492505050565b5f826106f257600190506107ad565b816106ff575f90506107ad565b8160018114610715576002811461071f5761074e565b60019150506107ad565b60ff84111561073157610730610658565b5b8360020a91508482111561074857610747610658565b5b506107ad565b5060208310610133831016604e8410600b84101617156107835782820a90508381111561077e5761077d610658565b5b6107ad565b6107908484846001610691565b925090508184048111156107a7576107a6610658565b5b81810290505b9392505050565b5f819050919050565b5f6107c7826107b4565b91506107d2836107b4565b92506107ff7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff84846106e3565b905092915050565b5f610811826107b4565b915061081c836107b4565b925082820261082a816107b4565b9150828204841483151761084157610840610658565b5b5092915050565b5f81519050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f60028204905060018216806108c357607f821691505b6020821081036108d6576108d561087f565b5b50919050565b5f819050815f5260205f209050919050565b5f6020601f8301049050919050565b5f82821b905092915050565b5f600883026109387fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff826108fd565b61094286836108fd565b95508019841693508086168417925050509392505050565b5f819050919050565b5f61097d610978610973846107b4565b61095a565b6107b4565b9050919050565b5f819050919050565b61099683610963565b6109aa6109a282610984565b848454610909565b825550505050565b5f90565b6109be6109b2565b6109c981848461098d565b505050565b5b818110156109ec576109e15f826109b6565b6001810190506109cf565b5050565b601f821115610a3157610a02816108dc565b610a0b846108ee565b81016020851015610a1a578190505b610a2e610a26856108ee565b8301826109ce565b50505b505050565b5f82821c905092915050565b5f610a515f1984600802610a36565b1980831691505092915050565b5f610a698383610a42565b9150826002028217905092915050565b610a8282610848565b67ffffffffffffffff811115610a9b57610a9a610852565b5b610aa582546108ac565b610ab08282856109f0565b5f60209050601f831160018114610ae1575f8415610acf578287015190505b610ad98582610a5e565b865550610b40565b601f198416610aef866108dc565b5f5b82811015610b1657848901518255600182019150602085019450602081019050610af1565b86831015610b335784890151610b2f601f891682610a42565b8355505b6001600288020188555050505b505050505050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610b7182610b48565b9050919050565b610b8181610b67565b82525050565b5f602082019050610b9a5f830184610b78565b92915050565b5f82825260208201905092915050565b7f495354436f696e3a2073656e64657220697320626c61636b6c697374656400005f82015250565b5f610be4601e83610ba0565b9150610bef82610bb0565b602082019050919050565b5f6020820190508181035f830152610c1181610bd8565b9050919050565b7f495354436f696e3a20726563697069656e7420697320626c61636b6c697374655f8201527f6400000000000000000000000000000000000000000000000000000000000000602082015250565b5f610c72602183610ba0565b9150610c7d82610c18565b604082019050919050565b5f6020820190508181035f830152610c9f81610c66565b9050919050565b5f610cb0826107b4565b9150610cbb836107b4565b9250828201905080821115610cd357610cd2610658565b5b92915050565b610ce2816107b4565b82525050565b5f606082019050610cfb5f830186610b78565b610d086020830185610cd9565b610d156040830184610cd9565b949350505050565b5f602082019050610d305f830184610cd9565b92915050565b61180680610d435f395ff3fe608060405234801561000f575f80fd5b50600436106100f3575f3560e01c806370a0823111610095578063a9059cbb11610064578063a9059cbb14610287578063dd62ed3e146102b7578063f2fde38b146102e7578063fe575a8714610303576100f3565b806370a0823114610211578063715018a6146102415780638da5cb5b1461024b57806395d89b4114610269576100f3565b806323b872dd116100d157806323b872dd14610163578063313ce5671461019357806344337ea1146101b1578063537df3b6146101e1576100f3565b806306fdde03146100f7578063095ea7b31461011557806318160ddd14610145575b5f80fd5b6100ff610333565b60405161010c91906111df565b60405180910390f35b61012f600480360381019061012a9190611290565b6103c3565b60405161013c91906112e8565b60405180910390f35b61014d6103e5565b60405161015a9190611310565b60405180910390f35b61017d60048036038101906101789190611329565b6103ee565b60405161018a91906112e8565b60405180910390f35b61019b61041c565b6040516101a89190611394565b60405180910390f35b6101cb60048036038101906101c691906113ad565b610431565b6040516101d891906112e8565b60405180910390f35b6101fb60048036038101906101f691906113ad565b6105d3565b60405161020891906112e8565b60405180910390f35b61022b600480360381019061022691906113ad565b610705565b6040516102389190611310565b60405180910390f35b61024961074a565b005b61025361075d565b60405161026091906113e7565b60405180910390f35b610271610785565b60405161027e91906111df565b60405180910390f35b6102a1600480360381019061029c9190611290565b610815565b6040516102ae91906112e8565b60405180910390f35b6102d160048036038101906102cc9190611400565b610837565b6040516102de9190611310565b60405180910390f35b61030160048036038101906102fc91906113ad565b6108b9565b005b61031d600480360381019061031891906113ad565b61093d565b60405161032a91906112e8565b60405180910390f35b6060600380546103429061146b565b80601f016020809104026020016040519081016040528092919081815260200182805461036e9061146b565b80156103b95780601f10610390576101008083540402835291602001916103b9565b820191905f5260205f20905b81548152906001019060200180831161039c57829003601f168201915b5050505050905090565b5f806103cd61098f565b90506103da818585610996565b600191505092915050565b5f600254905090565b5f806103f861098f565b90506104058582856109a8565b610410858585610a3b565b60019150509392505050565b5f60075f9054906101000a900460ff16905090565b5f61043a610b2b565b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036104a8576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161049f9061150b565b60405180910390fd5b60065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff1615610532576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161052990611599565b60405180910390fd5b600160065f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508173ffffffffffffffffffffffffffffffffffffffff167ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed62760405160405180910390a260019050919050565b5f6105dc610b2b565b60065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff16610665576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161065c90611627565b60405180910390fd5b5f60065f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508173ffffffffffffffffffffffffffffffffffffffff167f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d460405160405180910390a260019050919050565b5f805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20549050919050565b610752610b2b565b61075b5f610bb2565b565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b6060600480546107949061146b565b80601f01602080910402602001604051908101604052809291908181526020018280546107c09061146b565b801561080b5780601f106107e25761010080835404028352916020019161080b565b820191905f5260205f20905b8154815290600101906020018083116107ee57829003601f168201915b5050505050905090565b5f8061081f61098f565b905061082c818585610a3b565b600191505092915050565b5f60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905092915050565b6108c1610b2b565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610931575f6040517f1e4fbdf700000000000000000000000000000000000000000000000000000000815260040161092891906113e7565b60405180910390fd5b61093a81610bb2565b50565b5f60065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b5f33905090565b6109a38383836001610c75565b505050565b5f6109b38484610837565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff811015610a355781811015610a26578281836040517ffb8f41b2000000000000000000000000000000000000000000000000000000008152600401610a1d93929190611645565b60405180910390fd5b610a3484848484035f610c75565b5b50505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610aab575f6040517f96c6fd1e000000000000000000000000000000000000000000000000000000008152600401610aa291906113e7565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603610b1b575f6040517fec442f05000000000000000000000000000000000000000000000000000000008152600401610b1291906113e7565b60405180910390fd5b610b26838383610e44565b505050565b610b3361098f565b73ffffffffffffffffffffffffffffffffffffffff16610b5161075d565b73ffffffffffffffffffffffffffffffffffffffff1614610bb057610b7461098f565b6040517f118cdaa7000000000000000000000000000000000000000000000000000000008152600401610ba791906113e7565b60405180910390fd5b565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508160055f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f73ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff1603610ce5575f6040517fe602df05000000000000000000000000000000000000000000000000000000008152600401610cdc91906113e7565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610d55575f6040517f94280d62000000000000000000000000000000000000000000000000000000008152600401610d4c91906113e7565b60405180910390fd5b8160015f8673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20819055508015610e3e578273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92584604051610e359190611310565b60405180910390a35b50505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1614158015610e855750610e848361093d565b5b15610ec5576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610ebc906116c4565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614158015610f065750610f058261093d565b5b15610f46576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610f3d90611752565b60405180910390fd5b610f51838383610f56565b505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610fa6578060025f828254610f9a919061179d565b92505081905550611074565b5f805f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205490508181101561102f578381836040517fe450d38c00000000000000000000000000000000000000000000000000000000815260040161102693929190611645565b60405180910390fd5b8181035f808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550505b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036110bb578060025f8282540392505081905550611105565b805f808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f82825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef836040516111629190611310565b60405180910390a3505050565b5f81519050919050565b5f82825260208201905092915050565b8281835e5f83830152505050565b5f601f19601f8301169050919050565b5f6111b18261116f565b6111bb8185611179565b93506111cb818560208601611189565b6111d481611197565b840191505092915050565b5f6020820190508181035f8301526111f781846111a7565b905092915050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61122c82611203565b9050919050565b61123c81611222565b8114611246575f80fd5b50565b5f8135905061125781611233565b92915050565b5f819050919050565b61126f8161125d565b8114611279575f80fd5b50565b5f8135905061128a81611266565b92915050565b5f80604083850312156112a6576112a56111ff565b5b5f6112b385828601611249565b92505060206112c48582860161127c565b9150509250929050565b5f8115159050919050565b6112e2816112ce565b82525050565b5f6020820190506112fb5f8301846112d9565b92915050565b61130a8161125d565b82525050565b5f6020820190506113235f830184611301565b92915050565b5f805f606084860312156113405761133f6111ff565b5b5f61134d86828701611249565b935050602061135e86828701611249565b925050604061136f8682870161127c565b9150509250925092565b5f60ff82169050919050565b61138e81611379565b82525050565b5f6020820190506113a75f830184611385565b92915050565b5f602082840312156113c2576113c16111ff565b5b5f6113cf84828501611249565b91505092915050565b6113e181611222565b82525050565b5f6020820190506113fa5f8301846113d8565b92915050565b5f8060408385031215611416576114156111ff565b5b5f61142385828601611249565b925050602061143485828601611249565b9150509250929050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f600282049050600182168061148257607f821691505b6020821081036114955761149461143e565b5b50919050565b7f426c61636b6c697374416363657373436f6e74726f6c3a2063616e6e6f7420625f8201527f6c61636b6c69737420746865207a65726f206164647265737300000000000000602082015250565b5f6114f5603983611179565b91506115008261149b565b604082019050919050565b5f6020820190508181035f830152611522816114e9565b9050919050565b7f426c61636b6c697374416363657373436f6e74726f6c3a206163636f756e74205f8201527f616c726561647920626c61636b6c697374656400000000000000000000000000602082015250565b5f611583603383611179565b915061158e82611529565b604082019050919050565b5f6020820190508181035f8301526115b081611577565b9050919050565b7f426c61636b6c697374416363657373436f6e74726f6c3a206163636f756e74205f8201527f6e6f7420626c61636b6c69737465640000000000000000000000000000000000602082015250565b5f611611602f83611179565b915061161c826115b7565b604082019050919050565b5f6020820190508181035f83015261163e81611605565b9050919050565b5f6060820190506116585f8301866113d8565b6116656020830185611301565b6116726040830184611301565b949350505050565b7f495354436f696e3a2073656e64657220697320626c61636b6c697374656400005f82015250565b5f6116ae601e83611179565b91506116b98261167a565b602082019050919050565b5f6020820190508181035f8301526116db816116a2565b9050919050565b7f495354436f696e3a20726563697069656e7420697320626c61636b6c697374655f8201527f6400000000000000000000000000000000000000000000000000000000000000602082015250565b5f61173c602183611179565b9150611747826116e2565b604082019050919050565b5f6020820190508181035f83015261176981611730565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f6117a78261125d565b91506117b28361125d565b92508282019050808211156117ca576117c9611770565b5b9291505056fea26469706673582212204f8c4844cf40b7f5cc063419960a1590f05bbeda545ad7358b076e79057e2c0e64736f6c634300081a0033";

        // Now run ISTCoin
        Address istCoinAddress = Address.fromHexString("0x3328358128832A260C76A4141e19E2A943CD4B6D");
        simpleWorld.createAccount(istCoinAddress, 0, Wei.fromEth(0));
        MutableAccount istCoinAccount = (MutableAccount) simpleWorld.get(istCoinAddress);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        StandardJsonTracer tracer = new StandardJsonTracer(ps, true, true, true, true);

        // Deploy ISTCoin bytecode
        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        executor.code(Bytes.fromHexString(istCoinDeploymentBytecode));
        executor.sender(ownerAddress);
        executor.receiver(istCoinAddress);
        executor.worldUpdater(simpleWorld.updater());
        executor.execute();

        // Replace with runtime ISTCoin bytecode
        String runtime_bytecode = extractReturnData(baos);
        istCoinAccount.setCode(Bytes.fromHexString(runtime_bytecode));
        executor.code(istCoinAccount.getCode());

        System.out.println("Deployed ISTCoin at: " + istCoinAddress);
        System.out.println();

        // TEST 1: Transfer tokens from owner to user
        System.out.println("TEST 1: Transfer tokens from owner to user");

        String transferData = "a9059cbb" +
                padHexStringTo256Bit(userAddress.toString().substring(2)) +
                convertIntegerToHex256Bit(1);

        executor.callData(Bytes.fromHexString(transferData));
        executor.execute();

        // Check if transfer was successful (generally returns 1 for success)
        boolean transferSuccess = extractIntegerFromReturnData(baos).intValue() == 1;
        System.out.println("Transfer successful: " + transferSuccess);
        System.out.println();

        // TEST 2: Check balanceOf user
        System.out.println("TEST 2: Check balanceOf user");

        // balanceOf(address) = 70a08231
        String balanceOfData = "70a08231" + padHexStringTo256Bit(userAddress.toHexString().substring(2));

        executor.callData(Bytes.fromHexString(balanceOfData));
        executor.execute();

        int userBalance = extractIntegerFromReturnData(baos).intValue();
        System.out.println("User balance: " + userBalance);
        System.out.println();

        // TEST 3: Add user to blacklist
        System.out.println("TEST 3: Add user to blacklist");

        // addToBlacklist(address) = 44337ea1
        String blacklistData = "44337ea1" + padHexStringTo256Bit(userAddress.toHexString().substring(2));

        executor.callData(Bytes.fromHexString(blacklistData));
        executor.execute();

        boolean addedToBlacklist = extractIntegerFromReturnData(baos).intValue() == 1;
        System.out.println("Added to blacklist: " + addedToBlacklist);
        System.out.println();

        // TEST 4: Check if user is blacklisted
        System.out.println("TEST 4: Check if user is blacklisted");

        // isBlacklisted(address) = fe575a87
        String isBlacklistedData = "fe575a87" + padHexStringTo256Bit(userAddress.toHexString().substring(2));

        executor.callData(Bytes.fromHexString(isBlacklistedData));
        executor.execute();

        int blacklistStatus = extractIntegerFromReturnData(baos).intValue();
        System.out.println("Is user blacklisted: " + blacklistStatus);
        System.out.println();

        // TEST 5: Try to transfer from owner to blacklisted user (should fail)
        System.out.println("TEST 5: Try to transfer from owner to blacklisted user (should fail)");

        executor.callData(Bytes.fromHexString(transferData));
        executor.execute();

        // We can determine if the transaction reverted by checking the JSON trace for
        // error indication
        // This is a simplification - proper error detection would need more advanced
        // JSON parsing
        String traceOutput = baos.toString();
        boolean transferToBlacklistedFailed = traceOutput.contains("\"error\":")
                || traceOutput.contains("\"reverted\":");
        System.out.println("Transfer to blacklisted user failed as expected: " + transferToBlacklistedFailed);

        // We can extract revert reason by looking for specific patterns in the trace
        // output
        if (transferToBlacklistedFailed) {
            String revertReason = extractRevertReasonFromTrace(traceOutput);
            if (revertReason != null) {
                System.out.println("Revert reason: " + revertReason);
                System.out.println();
            }
        }

        // TEST 6: Try to transfer from blacklisted user to owner (should fail)
        System.out.println("TEST 6: Try to transfer from blacklisted user to owner (should fail)");

        // transfer 100 tokens back to owner
        String transferBackData = "a9059cbb" +
                padHexStringTo256Bit(ownerAddress.toHexString().substring(2)) +
                convertIntegerToHex256Bit(100);

        executor.callData(Bytes.fromHexString(transferBackData));
        executor.execute();

        // Check if this transfer failed as expected
        traceOutput = baos.toString();
        boolean transferFromBlacklistedFailed = traceOutput.contains("\"error\":")
                || traceOutput.contains("\"reverted\":");
        System.out.println("Transfer from blacklisted user failed as expected: " + transferFromBlacklistedFailed);

        if (transferFromBlacklistedFailed) {
            String revertReason = extractRevertReasonFromTrace(traceOutput);
            if (revertReason != null) {
                System.out.println("Revert reason: " + revertReason);
                System.out.println();
            }
        }

        // TEST 7: Remove user from blacklist
        System.out.println("TEST 7: Remove user from blacklist");

        // removeFromBlacklist(address) = 537df3b6
        String removeBlacklistData = "537df3b6" + padHexStringTo256Bit(userAddress.toHexString().substring(2));

        executor.callData(Bytes.fromHexString(removeBlacklistData));
        executor.execute();

        boolean removedFromBlacklist = extractIntegerFromReturnData(baos).intValue() == 1;
        System.out.println("Removed from blacklist: " + removedFromBlacklist);
        System.out.println();

        // TEST 8: Try transfer again after removing from blacklist
        System.out.println("TEST 8: Try transfer again after removing from blacklist");

        executor.callData(Bytes.fromHexString(transferBackData));
        executor.execute();

        boolean transferAfterRemovalSuccess = extractIntegerFromReturnData(baos).intValue() == 1;
        System.out.println("Transfer after blacklist removal success: " + transferAfterRemovalSuccess);
        System.out.println();

        // Final check of ISTCoin storage
        System.out.println("\nFinal ISTCoin storage state:");
        System.out.println("  Name: " + readStringFromStorage(simpleWorld, istCoinAddress, 3));
        System.out.println("  Symbol: " + readStringFromStorage(simpleWorld, istCoinAddress, 4));
        System.out.println("  Total Supply: " + simpleWorld.get(istCoinAddress).getStorageValue(UInt256.valueOf(2)));

        // Check balanceOf owner in storage
        String ownerBalanceKey = calculateMappingKey(ownerAddress.toHexString(), 0);
        System.out.println("  Owner Balance: "
                + simpleWorld.get(istCoinAddress).getStorageValue(UInt256.fromHexString(ownerBalanceKey)));

        // Check balanceOf user in storage
        String userBalanceKey = calculateMappingKey(userAddress.toHexString(), 0);
        System.out.println("  User Balance: "
                + simpleWorld.get(istCoinAddress).getStorageValue(UInt256.fromHexString(userBalanceKey)));
    }

    public static String extractReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        System.out.println("Size of content: " + byteArrayOutputStream.toString().length());
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        return memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
    }

    public static BigInteger extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();

        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        // Extract the hex string from memory
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        // Convert to BigInteger (supports large values)
        return new BigInteger(returnData, 16);
    }

    private static String extractRevertReasonFromTrace(String traceOutput) {
        // This is a simplified implementation - in reality, you would need more robust
        // parsing
        // Look for revert reason in output trace - this could vary based on how tracer
        // works
        if (traceOutput.contains("revertReason")) {
            // Try to extract from JSON
            try {
                String[] lines = traceOutput.split("\\r?\\n");
                for (String line : lines) {
                    if (line.contains("revertReason")) {
                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        if (json.has("revertReason")) {
                            return json.get("revertReason").getAsString();
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to simple string search if JSON parsing fails
                int start = traceOutput.indexOf("revertReason") + "revertReason".length() + 3; // Skip ": "
                int end = traceOutput.indexOf("\"", start);
                if (start > 0 && end > start) {
                    return traceOutput.substring(start, end);
                }
            }
        }

        // Default message if we couldn't extract a specific reason
        return "Transaction reverted";
    }

    private static String readStringFromStorage(SimpleWorld world, Address istCoinAddress, int slot) {
        // This is a simplified implementation - reading strings from storage is complex
        // In a real implementation, you'd need to handle both short and long strings
        return world.get(istCoinAddress).getStorageValue(UInt256.valueOf(slot)).toString();
    }

    private static String calculateMappingKey(String address, int mappingSlot) {
        if (address.startsWith("0x")) {
            address = address.substring(2);
        }

        String paddedAddress = padHexStringTo256Bit(address);
        String slotIndex = convertIntegerToHex256Bit(mappingSlot);

        return Numeric.toHexStringNoPrefix(
                Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + slotIndex)));
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);
        return String.format("%064x", bigInt);
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) + hexString;
    }
}