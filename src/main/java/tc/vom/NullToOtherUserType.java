package tc.vom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Created by matthias on 13.03.15.
 */
public class NullToOtherUserType implements UserType {

    private static Log LOG = LogFactory.getLog(NullToOtherUserType.class);
    @Override
    public int[] sqlTypes() {
        return new int[] {Types.CHAR};
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    public static String getValueToPersist(Object value){
        if(value == null){
            return "Unspecified";
        } else {
            return value.toString();
        }
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return getValueToPersist(x).equals(getValueToPersist(y));
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return getValueToPersist(x).hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return rs.getString(names[0]);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        LOG.info("I was called");
        st.setString(index, getValueToPersist(value));
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
}
