#shortcut to boot WildFly with ZKPing configuration
#if you need to set other boot options, please use standalone.sh

DIRNAME=`dirname "$0"`
REALPATH=`cd "$DIRNAME/../bin"; pwd`

${DIRNAME}/standalone.sh -c standalone-kube.xml
