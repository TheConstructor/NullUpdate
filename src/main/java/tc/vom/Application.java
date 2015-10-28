package tc.vom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.expression.LiteralExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.beans.PropertyVetoException;
import java.util.HashMap;

@SpringBootApplication
public class Application {

    private static Log LOG = LogFactory.getLog(Application.class);


    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        try (ConfigurableApplicationContext applicationContext = springApplication.run(args)) {
            TransactionTemplate transactionTemplate = applicationContext.getBean(TransactionTemplate.class);
            transactionTemplate.execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction(TransactionStatus status) {
                    EntityManager entityManager = applicationContext.getBean(EntityManager.class);

                    final Long id;
                    {
                        Entity entity = new Entity();
                        entity.setValue("Before update");
                        LOG.info("About to persist entity");
                        entity = entityManager.merge(entity);
                        id = entity.getId();
                    }

                    {
                        Entity entity = entityManager.find(Entity.class, id);
                        entityManager.refresh(entity);
                        LOG.info("Value after persisting: " + entity.getValue());
                        // Will be the value my UserType provides
                    }

                    {
                        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
                        CriteriaUpdate<Entity> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(Entity.class);
                        Root<Entity> root = criteriaUpdate.from(Entity.class);
                        criteriaUpdate.where(criteriaBuilder.equal(root.get("id"), id));
                        criteriaUpdate.set(root.get("value"), (Object) null);
                        Query query = entityManager.createQuery(criteriaUpdate);
                        LOG.info("About to perform 1. update");
                        LOG.info("Updated " + query.executeUpdate() + " entities");
                    }

                    {
                        Entity entity = entityManager.find(Entity.class, id);
                        entityManager.refresh(entity);
                        LOG.info("Value after 1. update: " + entity.getValue());
                        // Will be null
                        if (entity.getValue() == null) {
                            LOG.error(
                                    "UserType was not called to determine correct persistence-representation of null");
                        } else {
                            LOG.info("UserType was called to determine correct persistence-representation of null");
                        }
                    }

                    {
                        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
                        CriteriaUpdate<Entity> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(Entity.class);
                        Root<Entity> root = criteriaUpdate.from(Entity.class);
                        criteriaUpdate.where(criteriaBuilder.equal(root.get("id"), id));
                        setPathToNull((CriteriaBuilderImpl) criteriaBuilder, criteriaUpdate, root.get("value"));
                        Query query = entityManager.createQuery(criteriaUpdate);
                        LOG.info("About to perform 2. update");
                        LOG.info("Updated " + query.executeUpdate() + " entities");
                    }

                    {
                        Entity entity = entityManager.find(Entity.class, id);
                        entityManager.refresh(entity);
                        LOG.info("Value after 2. update: " + entity.getValue());
                        // Will be the value my UserType provides
                        if (entity.getValue() == null) {
                            LOG.error(
                                    "UserType was not called to determine correct persistence-representation of null");
                        } else {
                            LOG.info("UserType was called to determine correct persistence-representation of null");
                        }
                    }

                    return null;
                }

                private <Y> CriteriaUpdate<Entity> setPathToNull(CriteriaBuilderImpl criteriaBuilder, CriteriaUpdate<Entity> criteriaUpdate, Path<Y> value) {
                    return criteriaUpdate.set(value, new LiteralExpression<>(criteriaBuilder, (Y)null));
                }
            });
        }
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws PropertyVetoException {
        final LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactoryBean.setDataSource(dataSource());

        // Hibernate as persistence backend
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        final HashMap<String, String> jpaProperties = new HashMap<>();
        jpaProperties.put(AvailableSettings.DIALECT, H2Dialect.class.getName());
        jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, "update");
        jpaProperties.put(AvailableSettings.SHOW_SQL, "true");
        jpaProperties.put(AvailableSettings.FORMAT_SQL, "true");
        entityManagerFactoryBean.setJpaPropertyMap(jpaProperties);

        // No need for persistance.xml
        entityManagerFactoryBean.setPersistenceUnitName("quick-test");
        entityManagerFactoryBean.setPackagesToScan(Application.class.getPackage().getName());

        return entityManagerFactoryBean;
    }

    @Bean
    public SingleConnectionDataSource dataSource() throws PropertyVetoException {
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource("jdbc:h2:mem:quick-test", true);
        singleConnectionDataSource.setDriverClassName("org.h2.Driver");
        return singleConnectionDataSource;
    }

    @Bean
    @Autowired
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    @Autowired
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate;
    }
}
