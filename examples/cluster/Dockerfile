# Name: alesj/kubeping-cluster

FROM alesj/wildfly-kubeping

# ADD https://www.dropbox.com/s/ppug24nhmu3nz5f/hello-servlet-2.0-SNAPSHOT.war?dl=0 /opt/jboss/wildfly/standalone/deployments/hello.war
RUN curl -O -L https://www.dropbox.com/s/ppug24nhmu3nz5f/hello-servlet-2.0-SNAPSHOT.war
RUN mv hello-servlet-2.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/hello.war

# ADD hello-servlet-2.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/hello.war

USER root
RUN chown jboss:jboss /opt/jboss/wildfly/standalone/deployments/hello.war
