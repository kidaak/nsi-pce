package net.es.nsi.pce.pf.api.cons;

/**
 * Defines a basic PCE constraint modeling an number attribute with a name and
 * Long value.
 * 
 * @author hacksaw
 */
public class NumAttrConstraint extends AttrConstraint {
    private long value;

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}

