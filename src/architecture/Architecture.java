package architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import components.Bus;
import components.Demux;
import components.Memory;
import components.Register;
import components.Ula;

public class Architecture {
	
	private boolean simulation; //this boolean indicates if the execution is done in simulation mode.
								//simulation mode shows the components' status after each instruction
	
	
	private boolean halt;
	//bus
	private Bus intbus;
	private Bus extbus;
	
	private Memory memory;
	private Memory statusMemory;
	private int memorySize;
	private Register PC;
	private Register IR;
	
	//registers
	private Register REG0;
	private Register REG1;
	private Register REG2;
	private Register REG3;
	
	
	private Register Flags;

	private Ula ula;
	private Demux demux; //only for multiple register purposes
	
	//registers
	private ArrayList<String> commandsList;
	private ArrayList<Register> registersList;
	
	

	/**
	 * Instanciates all components in this architecture
	 */
	private void componentsInstances() {
		//don't forget the instantiation order
		//buses -> registers -> ula -> memory
		
		intbus = new Bus();
		extbus = new Bus();


		PC = new Register("PC", intbus, intbus);
		IR = new Register("IR", extbus, intbus);


		REG0 = new Register ("REG0", extbus, extbus);
		REG1 = new Register ("REG1", extbus, extbus);
		REG2 = new Register ("REG2", extbus, extbus);
		REG3 = new Register ("REG3", extbus, extbus);

		Flags = new Register(2, intbus);

		fillRegistersList();
		ula = new Ula(null, intbus);

		statusMemory = new Memory(2, extbus);
		memorySize = 128;
		memory = new Memory(memorySize, extbus);
		demux = new Demux(); //this bus is used only for multiple register operations

		
		
		fillCommandsList();
	}

	/**
	 * This method fills the registers list inserting into them all the registers we have.
	 * IMPORTANT!
	 * The first register to be inserted must be the default RPG
	 */
	private void fillRegistersList() {
		registersList = new ArrayList<Register>();
		registersList.add(REG0);
		registersList.add(REG1);
		registersList.add(REG2);
		registersList.add(REG3);
		
		registersList.add(PC);
		registersList.add(IR);
		registersList.add(Flags);
	}

	/**
	 * Constructor that instanciates all components according the architecture diagram
	 */
	public Architecture() {
		componentsInstances();
		
		//by default, the execution method is never simulation mode
		simulation = false;
	}

	
	public Architecture(boolean sim) {
		componentsInstances();
		
		//in this constructor we can set the simoualtion mode on or off
		simulation = sim;
	}



	//getters
	
	protected Bus getExtbus() {
		return extbus;
	}

	protected Bus getIntbus() {
		return intbus;
	}

	protected Memory getMemory() {
		return memory;
	}

	public Memory getStatusMemory() {
		return statusMemory;
	}

	public int getMemorySize() {
		return memorySize;
	}

	protected Register getPC() {
		return PC;
	}

	protected Register getIR() {
		return IR;
	}

	protected Register getREG0() {
		return REG0;
	}

	protected Register getREG1() {
		return REG1;
	}

	protected Register getREG2() {
		return REG2;
	}

	protected Register getREG3() {
		return REG3;
	}

	protected Register getFlags() {
		return Flags;
	}

	protected Ula getUla() {
		return ula;
	}

	public ArrayList<String> getCommandsList() {
		return commandsList;
	}

	public ArrayList<Register> getRegistersList() {
		return registersList;
	}

	public Demux getDemux() {
		return demux;
	}

	//all the microprograms must be impemented here
	//the instructions table is
	/*
	 *
			add addr (rpg <- rpg + addr)
			sub addr (rpg <- rpg - addr)
			jmp addr (pc <- addr)
			jz addr  (se bitZero pc <- addr)
			jn addr  (se bitneg pc <- addr)
			read addr (rpg <- addr)
			store addr  (addr <- rpg)
			ldi x    (rpg <- x. x must be an integer)
			inc    (rpg++)
			move regA regB (regA <- regB)
	 */
	
	/**
	 * This method fills the commands list arraylist with all commands used in this architecture
	 */
	protected void fillCommandsList() {
		commandsList = new ArrayList<String>();
		
		commandsList.add("addRegReg"); //0
		commandsList.add("addMemReg"); //1
		commandsList.add("addRegMem"); //2
		commandsList.add("addImmReg"); //3
		commandsList.add("subRegReg"); //4
		commandsList.add("subMemReg"); //5
		commandsList.add("subRegMem"); //6
		commandsList.add("subImmReg"); //7
		commandsList.add("imulMemReg"); //8 
		commandsList.add("imulRegMem"); //9 
		commandsList.add("imulRegReg"); //10 
		commandsList.add("moveMemReg"); //11
		commandsList.add("moveRegMem"); //12
		commandsList.add("moveRegReg"); //13
		commandsList.add("moveImmReg"); //14
		commandsList.add("incReg"); //15
		commandsList.add("jmp"); //16
		commandsList.add("jn"); //17
		commandsList.add("jz"); //18
		commandsList.add("jeq"); //19
		commandsList.add("jneq"); //20
		commandsList.add("jgt"); //21
		commandsList.add("jlw"); //22
	}

	
	/**
	 * This method is used after some ULA operations, setting the flags bits according the result.
	 * @param result is the result of the operation
	 * NOT TESTED!!!!!!!
	 */
	private void setStatusFlags(int result) {
		Flags.setBit(0, 0);
		Flags.setBit(1, 0);
		if (result==0) { //bit 0 in flags must be 1 in this case
			Flags.setBit(0,1);
		}
		if (result<0) { //bit 1 in flags must be 1 in this case
			Flags.setBit(1,1);
		}
	}

	/**
	 * This method implements the microprogram for
	 * 					ADD address
	 * In the machine language this command number is 0, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture
	 * The method reads the value from memory (position address) and 
	 * performs an add with this value and that one stored in the RPG (the first register in the register list).
	 * The final result must be in RPG (the first register in the register list).
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. rpg -> intbus1 //rpg.read() the current rpg value must go to the ula 
	 * 7. ula <- intbus1 //ula.store()
	 * 8. pc -> extbus (pc.read())
	 * 9. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 10. memory reads from extbus //this forces memory to write the data value in the extbus
	 * 11. rpg <- extbus (rpg.store())
	 * 12. rpg -> intbus1 (rpg.read())
	 * 13. ula  <- intbus1 //ula.store()
	 * 14. Flags <- zero //the status flags are reset
	 * 15. ula adds
	 * 16. ula -> intbus1 //ula.read()
	 * 17. ChangeFlags //informations about flags are set according the result 
	 * 18. rpg <- intbus1 //rpg.store() - the add is complete.
	 * 19. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	public void addRegReg() {
		//PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		
		IR.internalStore();
		IR.read();
		memory.read();
		demux.setValue(extbus.get());
		registersRead();
		IR.store();
		IR.internalRead();
		ula.store(0); //the rpg value is in ULA (0). This is the first parameter
		
		//PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 

		IR.internalStore();
		IR.read();
		memory.read();
		demux.setValue(extbus.get());
		registersRead();
		IR.store();
		IR.internalRead();
		ula.store(1); //the rpg value is in ULA (0). This is the first parameter

		ula.add(); //the result is in the second ula's internal register
		setStatusFlags(intbus.get());
		IR.internalStore();
		IR.read();
	
        registersStore(); //performs an internal store for the register identified into demux bus
		

		//PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 

	}
	
	public void addMemReg() {
	  //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address

	    // Lê o valor da memória
	    IR.internalStore();
		IR.read();
		memory.read();
		memory.read();

	    // Aguarda pelo valor do segundo registrador
	    IR.store();
		IR.internalRead();
		ula.store(0);

	      //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address

	    // Lê o ID do segundo registrador da memória
	    IR.internalStore();
		IR.read();
		memory.read();
		demux.setValue(extbus.get());
		registersRead();
		IR.store();
		IR.internalRead();

	
	    // Salva o valor do segundo registrador na ULA
	    ula.store(1);

	    // Adiciona o valor do primeiro registrador ao valor do segundo registrador
	    ula.add();
		setStatusFlags(intbus.get());

	     //the operation result is in the internalbus 2
		IR.internalStore();
		IR.read();

		registersStore(); //performs an internal store for the register identified into demux bus
		

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}

public void addRegMem() {
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
	    memory.read();
		demux.setValue(extbus.get());
		registersRead();
	
		IR.store();
		IR.internalRead();

	    ula.store(0);
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
	    memory.read();
	    memory.store();
	    memory.read();
		IR.store();
		IR.internalRead();
	    ula.store(1);
	    ula.add();
		IR.internalStore();
		IR.read();
	    memory.store();
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
	}

	/**
	 * This method implements the microprogram for
	 * 					SUB address
	 * In the machine language this command number is 1, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture
	 * The method reads the value from memory (position address) and 
	 * performs an SUB with this value and that one stored in the rpg (the first register in the register list).
	 * The final result must be in RPG (the first register in the register list).
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. rpg -> intbus1 //rpg.read() the current rpg value must go to the ula 
	 * 7. ula <- intbus1 //ula.store()
	 * 8. pc -> extbus (pc.read())
	 * 9. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 10. memory reads from extbus //this forces memory to write the data value in the extbus
	 * 11. rpg <- extbus (rpg.store())
	 * 12. rpg -> intbus1 (rpg.read())
	 * 13. ula  <- intbus1 //ula.store()
	 * 14. Flags <- zero //the status flags are reset
	 * 15. ula subs
	 * 16. ula -> intbus1 //ula.read()
	 * 17. ChangeFlags //informations about flags are set according the result 
	 * 18. rpg <- intbus1 //rpg.store() - the add is complete.
	 * 19. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	public void addImmReg() {
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
	  
	    // Lê o valor imediato da memória
	    memory.read();
		IR.store();
		IR.internalRead();
	    ula.store(0);
	  
	   //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
	  
	    // Lê o ID do registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();
	    demux.setValue(extbus.get());
	  
	    // Seleciona o registrador e lê seu valor
	    registersRead();
		IR.store();
		IR.internalRead();
	    ula.store(1);
	  
	    // Adiciona o valor imediato ao valor do registrador
	    ula.add();
		 setStatusFlags(intbus.get());
	  
	    // Escreve o resultado de volta no registrador
	    IR.internalStore();
	    IR.read();
	    demux.setValue(extbus.get());
	    registersStore();
	   
	  
	   //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
	}
	
	public void subRegReg() {
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		
		IR.internalStore();
		IR.read();
		memory.read();
		demux.setValue(extbus.get());
		registersRead();
		IR.store();
		IR.internalRead();
		ula.store(0); //the rpg value is in ULA (0). This is the first parameter
		
		//PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 

		IR.internalStore();
		IR.read();
		memory.read();
		demux.setValue(extbus.get());
		registersRead();
		IR.store();
		IR.internalRead();
		ula.store(1); //the rpg value is in ULA (0). This is the first parameter

		ula.sub(); //the result is in the second ula's internal register
		setStatusFlags(intbus.get());
		IR.internalStore();
		IR.read();
	
        registersStore(); //performs an internal store for the register identified into demux bus
		

		//PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 

	}

	public void subMemReg() {
	     //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address

	    // Lê o valor da memória
	    IR.internalStore();
		IR.read();
		memory.read();
		memory.read();

	    // Aguarda pelo valor do segundo registrador
	    IR.store();
		IR.internalRead();
		ula.store(0);

	      //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address

	    // Lê o ID do segundo registrador da memória
	    IR.internalStore();
		IR.read();
		memory.read();
		demux.setValue(extbus.get());
		registersRead();
		IR.store();
		IR.internalRead();

	
	    // Salva o valor do segundo registrador na ULA
	    ula.store(1);

	    // Adiciona o valor do primeiro registrador ao valor do segundo registrador
	    ula.sub();
		setStatusFlags(intbus.get());

	     //the operation result is in the internalbus 2
		IR.internalStore();
		IR.read();

		registersStore(); //performs an internal store for the register identified into demux bus
		

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}

	public void subRegMem() {
	      //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
	    memory.read();
		demux.setValue(extbus.get());
		registersRead();
	
		IR.store();
		IR.internalRead();

	    ula.store(0);
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
	    memory.read();
	    memory.store();
	    memory.read();
		IR.store();
		IR.internalRead();
	    ula.store(1);
	    ula.sub();
		IR.internalStore();
		IR.read();
	    memory.store();
	    //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
	}


	public void subImmReg() {
		 //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
	  
	    // Lê o valor imediato da memória
	    memory.read();
		IR.store();
		IR.internalRead();
	    ula.store(0);
	  
	   //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
	  
	    // Lê o ID do registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();
	    demux.setValue(extbus.get());
	  
	    // Seleciona o registrador e lê seu valor
	    registersRead();
		IR.store();
		IR.internalRead();
	    ula.store(1);
	  
	    // Adiciona o valor imediato ao valor do registrador
	    ula.sub();
		 setStatusFlags(intbus.get());
	  
	    // Escreve o resultado de volta no registrador
	    IR.internalStore();
	    IR.read();
	    demux.setValue(extbus.get());
	    registersStore();
	   
	  
	   //PC++
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
	}
	
	    
	// TESTANDO IMUL
	/*
	 public void imulMemReg() {
    // Incrementa o PC para apontar para o ID do registrador
    PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    PC.internalStore();
    
    // Lê o ID do registrador da memória
	IR.internalStore();
	IR.read();
    memory.read();
    demux.setValue(extbus.get()); // Direciona o barramento para selecionar o registrador
    registersRead(); // Lê o conteúdo do registrador
	IR.store();
	IR.internalRead();
    ula.store(1); // Armazena o valor lido no registrador na posição 1 da ULA
    
    // Incrementa o PC para apontar para o endereço da memória
    PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    PC.internalStore();
    
    // Lê o valor armazenado na memória
	IR.internalStore();
	IR.read();
    memory.read();
	IR.store();
	IR.internalRead();
    ula.store(0); // Armazena o valor da memória na posição 0 da ULA
    
    // Multiplica o valor do registrador pelo valor da memória
    ula.imul(); // Operação de multiplicação
    setStatusFlags(intbus.get()); // Define as flags conforme o resultado da multiplicação
    
    // Armazena o resultado na memória
	IR.internalStore();
	IR.read();
    memory.store();
    
    // Incrementa o PC para apontar para o próximo comando
    PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    PC.internalStore();
}
	 */
	

public void moveMemReg() {
	    // Incrementa o PC para apontar para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
        
		IR.internalStore();
		IR.read();
	    memory.read(); // Obtém o primeiro parâmetro
	    memory.read(); // Obtém o valor da posição de memória
	    IR.store();
		IR.internalRead();
		ula.store(0); // Salva o valor do extbus na ULA

	    // Incrementa o PC para apontar para o ID do registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

		IR.internalStore();
		IR.read();

	    memory.read(); // Obtém o ID do registrador
        
	    demux.setValue(extbus.get()); // Define o valor do demux como o ID do registrador
	    ula.read(0); // Move o valor da ULA para o extbus
		IR.internalStore();
		IR.read();
	    registersStore(); // Escreve o valor do extbus no registrador selecionado

	    // Incrementa o PC para apontar novamente para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}

	public void moveRegMem() {
	    // Incrementa o PC para apontar para o registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê o ID do registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();
		IR.store();
		IR.internalRead();
	    ula.store(0); // armazena o ID do registrador na ULA

	    // Incrementa o PC para apontar para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();
        IR.internalStore();
		IR.read();
	    memory.read(); // Obtém o valor da posição de memória
	    memory.store(); // Envia a posição de memória e espera pelo valor

	    // Move o valor da ULA para o extbus
	    ula.read(0);
		IR.internalStore();
		IR.read();

	    // Escreve o valor do extbus na memória
	    demux.setValue(extbus.get());
	    registersRead();

	    // Escreve o valor do extbus na memória
	    memory.store();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}

    public void moveRegReg() {
	    // Incrementa o PC para apontar para o primeiro registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Lê o ID do primeiro registrador da memória
	    IR.internalStore();
		IR.read();
		memory.read();
	    IR.store();
		IR.internalRead();
		ula.store(0); // armazena o ID do registrador na ULA

	    // Incrementa o PC para apontar para o segundo registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	    ula.read(0); // envia o valor do extbus de volta para o extbus
        IR.internalStore();
		IR.read();
	    // Seleciona o primeiro registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Salva o valor do primeiro registrador na ULA
	    ula.store(0);

	    // Move o valor do PC para o extbus
	    PC.internalRead();
	    IR.internalStore();
		IR.read();

	    // Lê o ID do segundo registrador da memória
	    memory.read();

	    // Seleciona o segundo registrador
	    demux.setValue(extbus.get());

	    // Move o valor do primeiro registrador que foi armazenado na ULA para o extbus
	    ula.read(0);
		IR.internalStore();
		IR.read();

	    // Escreve o valor da ULA no registrador selecionado
	    registersStore();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}

	void moveImmReg() {
	    // Incrementa o PC para apontar para o valor imediato
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê o valor imediato da memória
		IR.internalStore();
		IR.read();
	    memory.read();
		IR.store();
		IR.internalRead();
	    ula.store(0); // armazena o valor imediato na ULA

	    // Incrementa o PC para apontar para o registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê o ID do registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o registrador
	    demux.setValue(extbus.get());

	    // Move o valor imediato que foi armazenado na ULA para o extbus
	    
		ula.read(0);
		IR.internalStore();
		IR.read();

	    // Escreve o valor da ULA no registrador selecionado
	    registersStore();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}

	public void incReg() {
	    // Incrementa o PC para apontar para o registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê o ID do registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o registrador
	    demux.setValue(extbus.get());
	    registersRead();

		IR.store();
		IR.internalRead();

	    // Salva o valor do registrador selecionado na ULA
	    ula.store(1);

	    // Incrementa o valor na ULA
	    ula.inc();
		setStatusFlags(intbus.get());

	    // Move o valor da ULA para o extbus
	    ula.read(1);
		IR.internalStore();
		IR.read();
	     // Define as flags de acordo com o resultado

	    // Escreve o valor da ULA no registrador selecionado
	    registersStore();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();
	}



	/**
	 * This method implements the microprogram for
	 * 					JMP address
	 * In the machine language this command number is 2, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where the PC is redirecto to)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register.
	 * So, the program is deviated
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //pc.read()
	 * 7. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 8. pc <- extbus //pc.store() //pc was pointing to another part of the memory
	 * end
	 * @param address
	 */
	public void jmp() {
	PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    
    PC.internalStore();

	IR.internalStore();
	IR.read();
    memory.read();
	IR.store();
	IR.internalRead();
    
    PC.internalStore();
  }
	
	/**
	 * This method implements the microprogram for
	 * 					JZ address
	 * In the machine language this command number is 3, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where 
	 * the PC is redirected to, but only in the case the ZERO bit in Flags is 1)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register if the ZERO bit in Flags register is setted.
	 * So, the program is deviated conditionally
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.internalstore() now pc points to the parameter
	 * 6. pc -> extbus1 //pc.read() now the parameter address is in the extbus1
	 * 7. Memory -> extbus1 //memory.read() the address (if jn) is in external bus 1
	 * 8. statusMemory(1)<- extbus1 // statusMemory.storeIn1()
	 * 9. ula incs
	 * 10. ula -> intbus2 //ula.read()
	 * 11. PC <- intbus2 // PC.internalStore() PC is now pointing to next instruction
	 * 12. PC -> extbus1 // PC.read() the next instruction address is in the extbus
	 * 13. statusMemory(0)<- extbus1 // statusMemory.storeIn0()
	 * 14. Flags(bitZero) -> extbus1 //the ZERO bit is in the external bus
	 * 15. statusMemory <- extbus // the status memory returns the correct address according the ZERO bit
	 * 16. PC <- extbus1 // PC stores the new address where the program is redirected to
	 * end
	 * @param address
	 */

	public void jn() {
    PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    
    PC.internalStore();
    
	IR.internalStore();
	IR.read();
    memory.read();
    statusMemory.storeIn1();

    PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    
    PC.internalStore();
	
	IR.internalStore();
	IR.read();
    statusMemory.storeIn0();

    extbus.put(Flags.getBit(1));
    statusMemory.read();

    IR.store();
	IR.internalRead();

    PC.internalStore();
  }
	public void jz() {
	PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    PC.internalStore();
    
	IR.internalStore();
	IR.read();
    memory.read();
    statusMemory.storeIn1();

    PC.internalRead();
    ula.internalStore(1);
    ula.inc();
    ula.internalRead(1);
    
    PC.internalStore();
    IR.internalStore();
	IR.read();
	statusMemory.storeIn0();

    extbus.put(Flags.getBit(0));
    statusMemory.read();

    IR.store();
	IR.internalRead();

    PC.internalStore();
  }
	
	
	/**
	 * This method implements the microprogram for
	 * 					jn address
	 * In the machine language this command number is 4, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where 
	 * the PC is redirected to, but only in the case the NEGATIVE bit in Flags is 1)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register if the NEG bit in Flags register is setted.
	 * So, the program is deviated conditionally
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.internalstore() now pc points to the parameter
	 * 6. pc -> extbus1 //pc.read() now the parameter address is in the extbus1
	 * 7. Memory -> extbus1 //memory.read() the address (if jn) is in external bus 1
	 * 8. statusMemory(1)<- extbus1 // statusMemory.storeIn1()
	 * 9. ula incs
	 * 10. ula -> intbus2 //ula.read()
	 * 11. PC <- intbus2 // PC.internalStore() PC is now pointing to next instruction
	 * 12. PC -> extbus1 // PC.read() the next instruction address is in the extbus
	 * 13. statusMemory(0)<- extbus1 // statusMemory.storeIn0()
	 * 14. Flags(bitNEGATIVE) -> extbus1 //the NEGATIVE bit is in the external bus
	 * 15. statusMemory <- extbus // the status memory returns the correct address according the ZERO bit
	 * 16. PC <- extbus1 // PC stores the new address where the program is redirected to
	 * end
	 * @param address
	 
	public void jeq() {
	    // Incrementa o PC para apontar para o primeiro registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();
		IR.internalStore();
		IR.read();

	    // Lê o ID do primeiro registrador da memória
	    memory.read();

	    // Seleciona o primeiro registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Aguarda o valor do segundo registrador
		IR.store();
		IR.internalRead();
	    ula.store(0);

	    // Incrementa o PC para apontar para o segundo registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Lê o ID do segundo registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o segundo registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Salva o valor do segundo registrador na ULA
		IR.store();
		IR.internalRead();
	    ula.store(1);

	    // Subtrai o valor do primeiro registrador do valor do segundo registrador
	    ula.sub();
	    setStatusFlags(intbus.get());
	    IR.internalStore();
		IR.read();
	  // Define as flags de acordo com o resultado

	    // Incrementa o PC para apontar para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Lê a posição de memória da memória
	    IR.internalStore();
		IR.read();
		memory.read();

	    // Armazena o status na memória
	    statusMemory.storeIn1();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Armazena o status na memória
	    IR.internalStore();
		IR.read();
		statusMemory.storeIn0();

	    // Coloca o bit 0 no extbus para leitura
	    extbus.put(flags.getBit(0));
	    statusMemory.read();

	    IR.store();
		IR.internalRead();
	    PC.internalStore();
	}

	public void jneq() {
	    // Incrementa o PC para apontar para o primeiro registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê o ID do primeiro registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o primeiro registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Armazena o valor do primeiro registrador na ULA
		IR.store();
		IR.internalRead();
	    ula.store(0);

	    // Incrementa o PC para apontar para o segundo registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê o ID do segundo registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o segundo registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Armazena o valor do segundo registrador na ULA
		IR.store();
		IR.internalRead();
	    ula.store(1);

	    // Subtrai o valor do primeiro registrador do valor do segundo registrador
	    ula.sub();
	    ula.internalRead(1);
	    setStatusFlags(intbus.get()); // Define as flags de acordo com o resultado

	    // Incrementa o PC para apontar para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    PC.internalStore();

	    // Lê a posição de memória da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Armazena a posição de memória para saltar em statusMemory
	    statusMemory.storeIn1();

	    // Incrementa o PC para apontar para a próxima instrução
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Armazena a posição de memória para saltar em statusMemory
		IR.internalStore();
		IR.read();
	    statusMemory.storeIn0();

	    // Verifica se o resultado da subtração não é zero (ou seja, os registradores não são iguais)
	    extbus.put(flags.getBit(0));
	    statusMemory.read();

	    IR.store();
		IR.internalRead();
	    PC.internalStore();
	}

	public void jgt() {
	    // Incrementa o PC para apontar para o primeiro registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Lê o ID do primeiro registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o primeiro registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Aguarda o valor do segundo registrador
		IR.store();
		IR.internalRead();
	    ula.store(0);

	    // Incrementa o PC para apontar para o segundo registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Lê o ID do segundo registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o segundo registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Salva o valor do segundo registrador na ULA
		IR.store();
		IR.internalRead();
	    ula.store(1);

	    // Subtrai o valor do primeiro registrador do valor do segundo registrador
	    ula.sub();
	    ula.internalRead(1);
	    setStatusFlags(intbus.get()); // Define as flags de acordo com o resultado

	    // Incrementa o PC para apontar para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Lê a posição de memória da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Armazena o status na memória
	    statusMemory.storeIn1();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	    
	    PC.internalStore();

	    // Armazena o status na memória
		IR.internalStore();
		IR.read();
	    statusMemory.storeIn0();

	    // Verifica se é > e não >=
	    extbus.put(flags.getBit(1));
		IR.store();
		IR.internalRead();
	    ula.store(0);
	    extbus.put(flags.getBit(0));
		IR.store();
		IR.internalRead();
	    ula.store(1);
	    ula.sub(); // Se o resultado for 0, o primeiro registrador é maior que o segundo e não igual
	    ula.internalRead(1);
	    setStatusFlags(intbus.get());
	    extbus.put(flags.getBit(0));
	    statusMemory.read();

	    IR.store();
		IR.internalRead();
	    PC.internalStore();
	}

	public void jlw() {
	    // Incrementa o PC para apontar para o primeiro registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	   
	    PC.internalStore();

	    // Lê o ID do primeiro registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o primeiro registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Aguarda o valor do segundo registrador
		IR.store();
		IR.internalRead();
	    ula.store(0);

	    // Incrementa o PC para apontar para o segundo registrador
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	   
	    PC.internalStore();

	    // Lê o ID do segundo registrador da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Seleciona o segundo registrador e lê seu valor
	    demux.setValue(extbus.get());
	    registersRead();

	    // Salva o valor do segundo registrador na ULA
		IR.store();
		IR.internalRead();
	    ula.store(1);

	    // Subtrai o valor do primeiro registrador do valor do segundo registrador
	    ula.sub();
	    ula.internalRead(1);
	    setStatusFlags(intbus.get()); // Define as flags de acordo com o resultado

	    // Incrementa o PC para apontar para a posição de memória
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	   
	    PC.internalStore();

	    // Lê a posição de memória da memória
		IR.internalStore();
		IR.read();
	    memory.read();

	    // Armazena o status na memória
	    statusMemory.storeIn1();

	    // Incrementa o PC para apontar para o próximo comando
	    PC.internalRead();
	    ula.internalStore(1);
	    ula.inc();
	    ula.internalRead(1);
	   
	    PC.internalStore();

	    // Armazena o status na memória
		IR.internalStore();
		IR.read();
	    statusMemory.storeIn0();

	    // Verifica se é > e não >=
	    extbus.put(flags.getBit(1));
		IR.store();
		IR.internalRead();
	    ula.store(0);
	    extbus.put(flags.getBit(0));
		IR.store();
		IR.internalRead();
	    ula.store(1);
	    ula.sub(); // Se o resultado não for negativo e não for zero, o primeiro registrador é menor que o segundo
	    ula.internalRead(1);
	    setStatusFlags(intbus.get());

	    extbus.put(flags.getBit(1));
		IR.store();
		IR.internalRead();
	    ula.store(0);
	    extbus.put(flags.getBit(0));
		IR.store();
		IR.internalRead();
	    ula.store(1);
	    ula.sub(); // Se o resultado for 0, o primeiro registrador é maior que o segundo e não igual
	    ula.internalRead(1);
	    setStatusFlags(intbus.get());
	    extbus.put(flags.getBit(0));
	    statusMemory.read();

	    IR.store();
		IR.internalRead();
	    PC.internalStore();
	}
	*/
	/**
	 * This method implements the microprogram for
	 * 					read address
	 * In the machine language this command number is 5, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture 
	 * The method reads the value from memory (position address) and 
	 * inserts it into the RPG register (the first register in the register list)
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the address in the extbus
	 * 8. memory reads from extbus //this forces memory to write the stored data in the extbus
	 * 9. RPG <- extbus //the data is read
	 * 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 11. ula <- intbus2 //ula.store()
	 * 12. ula incs
	 * 13. ula -> intbus2 //ula.read()
	 * 14. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 
	 public void read() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		IR.internalStore();
		IR.read();
		memory.read(); // the address is now in the external bus.
		memory.read(); // the data is now in the external bus.
		REG.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	 
	 
	 */
	
	
	/**
	 * This method implements the microprogram for
	 * 					store address
	 * In the machine language this command number is 6, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture 
	 * The method reads the value from RPG (the first register in the register list) and 
	 * inserts it into the memory (position address) 
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //(pc.read())the parameter address is the external bus
	 * 7. memory reads // memory reads the data in the parameter address. 
	 * 					// this data is the address where the RPG value must be stores 
	 * 8. memory stores //memory reads the address and wait for the value
	 * 9. RPG -> Externalbus //RPG.read()
	 * 10. memory stores //memory receives the value and stores it
	 * 11. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 12. ula <- intbus2 //ula.store()
	 * 13. ula incs
	 * 14. ula -> intbus2 //ula.read()
	 * 15. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */
	
	
	/**
	 * This method implements the microprogram for
	 * 					ldi immediate
	 * In the machine language this command number is 7, and the immediate value
	 *        is in the position next to him
	 *    
	 * The method moves the value (parameter) into the internalbus1 and the RPG 
	 * (the first register in the register list) consumes it 
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the stored data in the extbus
	 * 8. RPG <- extbus //rpg.store()
	 * 9. 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 10. ula <- intbus2 //ula.store()
	 * 11. ula incs
	 * 12. ula -> intbus2 //ula.read()
	 * 13. pc <- intbus2 //pc.store() 
	 * end
	 * @param address
	 */

	
	/**
	 * This method implements the microprogram for
	 * 					inc 
	 * In the machine language this command number is 8
	 *    
	 * The method moves the value in rpg (the first register in the register list)
	 *  into the ula and performs an inc method
	 * 		-> inc works just like add rpg (the first register in the register list)
	 *         with the mumber 1 stored into the memory
	 * 		-> however, inc consumes lower amount of cycles  
	 * 
	 * The logic is
	 * 
	 * 1. rpg -> intbus1 //rpg.read()
	 * 2. ula  <- intbus1 //ula.store()
	 * 3. Flags <- zero //the status flags are reset
	 * 4. ula incs
	 * 5. ula -> intbus1 //ula.read()
	 * 6. ChangeFlags //informations about flags are set according the result
	 * 7. rpg <- intbus1 //rpg.store()
	 * 8. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 9. ula <- intbus2 //ula.store()
	 * 10. ula incs
	 * 11. ula -> intbus2 //ula.read()
	 * 12. pc <- intbus2 //pc.store()
	 * end
	 * @param address
	 */

	
	/**
	 * This method implements the microprogram for
	 * 					move <reg1> <reg2> 
	 * In the machine language this command number is 9
	 *    
	 * The method reads the two register ids (<reg1> and <reg2>) from the memory, in positions just after the command, and
	 * copies the value from the <reg1> register to the <reg2> register
	 * 
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the first parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the parameter (first regID) in the extbus
	 * 8. pc -> intbus2 //pc.read() //getting the second parameter
	 * 9. ula <-  intbus2 //ula.store()
	 * 10. ula incs
	 * 11. ula -> intbus2 //ula.read()
	 * 12. pc <- intbus2 //pc.store() now pc points to the second parameter
	 * 13. demux <- extbus //now the register to be operated is selected
	 * 14. registers -> intbus1 //this performs the internal reading of the selected register 
	 * 15. PC -> extbus (pc.read())the address where is the position to be read is now in the external bus 
	 * 16. memory reads from extbus //this forces memory to write the parameter (second regID) in the extbus
	 * 17. demux <- extbus //now the register to be operated is selected
	 * 18. registers <- intbus1 //thid rerforms the external reading of the register identified in the extbus
	 * 19. 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store()  
	 * 		  
	 */
	
	
	
	public ArrayList<Register> getRegistersList1() {
		return registersList;
	}

	/**
	 * This method performs an (external) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersRead() {
		registersList.get(demux.getValue()).read();
	}
	
	/**
	 * This method performs an (external) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersStore() {
		registersList.get(demux.getValue()).store();
	}
	
	/**
	 * This method reads an entire file in machine code and
	 * stores it into the memory
	 * NOT TESTED
	 * @param filename
	 * @throws IOException 
	 */
	@SuppressWarnings("resource")
	public void readExec(String filename) throws IOException {
	    BufferedReader br = new BufferedReader(new FileReader(filename + ".dxf"));
	    String linha;
	    int i = 0;
	    
	    File file = new File("C:/Users/igorc/Downloads/S_Architecture/program.dxf");
	    if (!file.exists()) {
	        throw new FileNotFoundException("Arquivo não encontrado: " + file.getAbsolutePath());
	    }

	    while ((linha = br.readLine()) != null) {
	        // Inicializa o extbus1 antes de usá-lo
	        Bus extbus1 = new Bus(); 
	        
	        extbus1.put(i);            // Coloca o valor de 'i' no barramento
	        memory.store();           // Armazena na memória
	        
	        extbus1.put(Integer.parseInt(linha)); // Coloca o valor da linha no barramento
	        memory.store();           // Armazena novamente na memória
	        
	        i++;                      // Incrementa 'i'
	    }
	    br.close();
	}

	
	/**
	 * This method executes a program that is stored in the memory
	 */
	public void controlUnitEexec() {
		halt = false;
		while (!halt) {
			fetch();
			decodeExecute();
		}

	}
	

	/**
	 * This method implements The decode proccess,
	 * that is to find the correct operation do be executed
	 * according the command.
	 * And the execute proccess, that is the execution itself of the command
	 */
	private void decodeExecute() {
		IR.internalRead(); //the instruction is in the internalbus2
		int command = intbus.get();
		simulationDecodeExecuteBefore(command);
		switch (command) {
		case 2:
			jmp();
			break;
		case 3:
			jz();
			break;
		case 4:
			jn();
			break;
		case 9:
			moveRegReg();
			break;
		default:
			halt = true;
			break;
		}
		if (simulation)
			simulationDecodeExecuteAfter();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED
	 * @param command 
	 */
	private void simulationDecodeExecuteBefore(int command) {
		System.out.println("----------BEFORE Decode and Execute phases--------------");
		String instruction;
		int parameter = 0;
		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}
		if (command !=-1)
			instruction = commandsList.get(command);
		else
			instruction = "END";
		if (hasOperands(instruction)) {
			parameter = memory.getDataList()[PC.getData()+1];
			System.out.println("Instruction: "+instruction+" "+parameter);
		}
		else
			System.out.println("Instruction: "+instruction);
		if ("read".equals(instruction))
			System.out.println("memory["+parameter+"]="+memory.getDataList()[parameter]);
		
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED 
	 */
	private void simulationDecodeExecuteAfter() {
		String instruction = "Olá";
		System.out.println("-----------AFTER Decode and Execute phases--------------");
		System.out.println("Internal Bus 1: "+intbus.get());
		System.out.println("Internal Bus 2: "+intbus.get());
		System.out.println("External Bus 1: "+extbus.get());
		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}
		Scanner entrada = new Scanner(System.in);
		System.out.println("Press <Enter>");
		String mensagem = entrada.nextLine();
		System.out.println(mensagem + instruction);
		
		entrada.close();
	}

	/**
	 * This method uses PC to find, in the memory,
	 * the command code that must be executed.
	 * This command must be stored in IR
	 * NOT TESTED!
	 */
	private void fetch() {
		PC.read();
		memory.read();
		IR.store();
		simulationFetch();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED!!!!!!!!!
	 */
	private void simulationFetch() {
		if (simulation) {
			System.out.println("-------Fetch Phase------");
			System.out.println("PC: "+PC.getData());
			System.out.println("IR: "+IR.getData());
		}
	}

	/**
	 * This method is used to show in a correct way the operands (if there is any) of instruction,
	 * when in simulation mode
	 * NOT TESTED!!!!!
	 * @param instruction 
	 * @return
	 */
	private boolean hasOperands(String instruction) {
		if ("inc".equals(instruction)) //inc is the only one instruction having no operands
			return false;
		else
			return true;
	}

	/**
	 * This method returns the amount of positions allowed in the memory
	 * of this architecture
	 * NOT TESTED!!!!!!!
	 * @return
	 */
	public int getMemorySize1() {
		return memorySize;
	}
	
	public static void main(String[] args) throws IOException {
		Architecture arch = new Architecture(true);
		arch.readExec("program");
		arch.controlUnitEexec();
	}
	}
	


