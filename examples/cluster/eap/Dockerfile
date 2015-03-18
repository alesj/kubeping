# JBoss EAP with KubePING impl and examples.
# Name: 10.245.2.2:5000/dward/eap-kubeping-examples:v1

FROM 10.245.2.2:5000/dward/eap-kubeping:v1

USER root

#RUN chmod 644 /vagrant/openshift.local.certificates/kube-client/key.key

ADD standalone-kube.xml /opt/jboss/eap/standalone/configuration/standalone-kube.xml
RUN chown jboss:jboss /opt/jboss/eap/standalone/configuration/standalone-kube.xml

ADD basic-app-cache.war /opt/jboss/eap/standalone/deployments/basic-app-cache.war
RUN chown jboss:jboss /opt/jboss/eap/standalone/deployments/basic-app-cache.war

ADD basic-web-session.war /opt/jboss/eap/standalone/deployments/basic-web-session.war
RUN chown jboss:jboss /opt/jboss/eap/standalone/deployments/basic-web-session.war

ADD hello-servlet.war /opt/jboss/eap/standalone/deployments/hello-servlet.war
RUN chown jboss:jboss /opt/jboss/eap/standalone/deployments/hello-servlet.war

USER jboss
