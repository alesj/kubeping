# JBoss WildFly with KubePING impl and examples.
# Name: 10.245.2.2:5000/dward/wildfly-kubeping-examples:v1

FROM 10.245.2.2:5000/dward/wildfly-kubeping:v1

# ADD https://www.dropbox.com/s/ppug24nhmu3nz5f/hello-servlet-2.0-SNAPSHOT.war?dl=0 /opt/jboss/wildfly/standalone/deployments/hello.war
RUN curl -O -L https://www.dropbox.com/s/ppug24nhmu3nz5f/hello-servlet-2.0-SNAPSHOT.war
RUN mv hello-servlet-2.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/hello.war

# ADD hello-servlet-2.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/hello.war

USER root
RUN chown jboss:jboss /opt/jboss/wildfly/standalone/deployments/hello.war
