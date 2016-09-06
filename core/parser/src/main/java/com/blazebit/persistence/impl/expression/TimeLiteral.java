package com.blazebit.persistence.impl.expression;

import java.util.Date;

/**
 *
 * @author Moritz Becker
 * @since 1.2
 */
public class TimeLiteral extends TemporalLiteral {

    public TimeLiteral(Date value) {
        super(value);
    }

    @Override
    public Expression clone() {
        return new TimeLiteral(value);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(ResultVisitor<T> visitor) {
        return visitor.visit(this);
    }

}