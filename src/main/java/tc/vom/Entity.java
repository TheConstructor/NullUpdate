package tc.vom;

import org.hibernate.annotations.Type;

import javax.persistence.Basic;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by matthias on 13.03.15.
 */
@javax.persistence.Entity
public class Entity {


    private Long id;
    private String value;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Basic
    @Type(type = "tc.vom.NullToOtherUserType")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
