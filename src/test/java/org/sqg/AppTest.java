package org.sqg;

import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Persistence;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppTest.class);

    private static final class ResourceUtils {

        public static void close(AutoCloseable ...resources) throws Exception {
            if (resources != null) {
                for (AutoCloseable resource : resources) {
                    if (resource != null)
                        resource.close();
                }
            }
        }
    }

    @BeforeClass
    public static void setUp() {
        System.setProperty("org.jboss.logging.provider", "slf4j");
    }
    
    @Entity
    @Table(catalog = "", name = "student")
    static final class Student {

        @Id
        private int id;
        
        private String name;
        
        private int age;
        
        @Column(name = "class")
        private int clazz;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getClazz() {
            return clazz;
        }

        public void setClazz(int clazz) {
            this.clazz = clazz;
        }
        
        @Override
        public String toString() {
            return this.getClass().getName() + "@" + String.format("0x%08X", System.identityHashCode(this)) + "{id = " + id + ", name = " + name + ", age = " + age + ", class = " + clazz + "}";
        }
    }

    @Test
    public void testJDBC() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource("sample.sqlite");
        final String CONNECTION_STRING = String.format("jdbc:sqlite:%s", Paths.get(url.toURI()));
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(CONNECTION_STRING);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM student");
            int count = 0;
            while (rs.next()) {
                ++count;
            }
            LOGGER.info("There're {} records!", count);
        } finally {
            ResourceUtils.close(rs, stmt, conn);
        }
    }

    @Test
    public void testJPA() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource("sample.sqlite");
        final String CONNECTION_STRING = String.format("jdbc:sqlite:%s", Paths.get(url.toURI()));
        Map<String, String> settings = new HashMap<>();
        // settings.put("javax.persistence.jdbc.driver", "org.sqlite.JDBC");
        settings.put("javax.persistence.jdbc.url", CONNECTION_STRING);
        settings.put("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect");
        final String JPQL = "SELECT s FROM org.sqg.AppTest$Student s";

        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory("User", settings);
            em = emf.createEntityManager();
            Student s = em.find(Student.class, 2);
            if (s == null)
                return;
            LOGGER.info("origin is {}", s);
            s.setAge(s.getAge() + 1);
            em.getTransaction().begin();
            em.merge(s);
            em.getTransaction().commit();
            LOGGER.info("update to {}", s);
            em.refresh(s);
            LOGGER.info("refresh to {}", s);
            em.getTransaction().begin();
            s.setAge(s.getAge() - 1);
            em.merge(s);
            em.getTransaction().commit();
            LOGGER.info("update to {}", s);
            em.refresh(s);
            LOGGER.info("refresh to {}", s);
            TypedQuery<Student> query = em.createQuery(JPQL, Student.class);
            List<Student> students = query.getResultList();
            for (Student student : students)
                LOGGER.info("{}", student);
        } finally {
            if (em != null) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
                em = null;
            }
            if (emf != null) {
                emf.close();
                emf = null;
            }
        }
    }
}
