// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.compiler;

import pspdash.data.SimpleData;

abstract class UnaryOperator implements Instruction {

    private String operatorString = null;

    UnaryOperator(String op) {
        operatorString = op;
    }

    public void execute(Stack stack, ExpressionContext context)
        throws ExecutionException
    {
        Object operand = null;
        try {
            operand = stack.pop();
        } catch (Exception e) {
            throw new ExecutionException("execution stack is empty");
        }
        try {
            stack.push(operate((SimpleData) operand));
        } catch (ClassCastException cce) {
            throw new ExecutionException("ClassCastException");
        }
    }

    protected abstract SimpleData operate(SimpleData operand);

    public String toString() { return operatorString; }
}
