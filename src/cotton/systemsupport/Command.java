/*

Copyright (c) 2016, Gunnlaugur Juliusson, Jonathan Kåhre, Magnus Lundmark,
Mats Levin, Tony Tran
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Cotton Production Team nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */
package cotton.systemsupport;

import java.io.Serializable;

/**
 *
 * @author Tony
 * @author Magnus
 */
public class Command implements Serializable {

    private final StatType type;
    private final String name;
    private final String[] tokens;
    private final int amount;
    private final CommandType command;
    private boolean query = false;

    /**
     * This class creates a command that can be sent to notify other nodes.
     *
     * @param type
     * @param name
     * @param tokens
     * @param amount
     * @param command
     */
    public Command(StatType type, String name,String[] tokens, int amount, CommandType command) {
        this.type = type;
        this.name = name;
        this.tokens = tokens;
        this.amount = amount;
        this.command = command;
    }

    /**
     * returns the type of the given subsystem this command was created for.
     *
     * @return type The type of the subsystem.
     */
    public StatType getType() {
        return type;
    }

    /**
     * returns the name of the given subsystem.
     *
     * @return name the name of the section within a subsystem.
     */
    public String getName() {
        return name;
    }

    /**
     * returns a value that should be changed.
     *
     * @return amount the value that should be changed on a subsystem
     */
    public int getAmount() {
        return amount;
    }

    /**
     * returns a specific command that instructs a subsystem what to do.
     *
     * @return command
     */
    public CommandType getCommandType() {
        return command;
    }

    /**
     * returns a sequence of tokens that should be executed.
     * @return tokens  
     */
    public String[] getTokens() {
        return tokens;
    }


    /**
     * whether the command should respond back to origin.
     *
     * @return true or false whether the command should respond back to origin.
     */
    public boolean isQuery() {
        return this.query;
    }

    /**
     * Sets whether the command should respond back to origin.
     * 
     * @param response true gives response and default false.
     */
    public void setQuery(boolean response) {
        this.query = response;
    }

}
