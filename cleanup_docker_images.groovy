// Put this script in a pipeline job in Jenkins
// Schedule it to run as often as needed (every 3 hours etc.)
// It will then remove unused docker containers and images
node {
  wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
    wrap([$class: 'TimestamperBuildWrapper']) {
      
// Load the next part from source control
// load "cleanup_docker.groovy"
      
import hudson.model.*

// Get slaves tagged 'docker' from Jenkins
def slaves = []
for (slave in Hudson.getInstance().getNodes()) {
  if (slave.getLabelString() == 'docker') {
    def slaveStr = slave.getDisplayName()
      slaves.push(slaveStr)
  }
}

stage "Cleanup Docker images on $slaves"
def parallelSteps = [:]
for (slave in slaves) {
  def runner = slave
  parallelSteps[slave] = {
    node(runner) {
      // Rebuild latest base images on slave
      // then do cleanup
      try {
        sh '''
          docker pull ubuntu:14.04
          docker pull ubuntu:16.04
          docker pull centos:6
          docker pull centos:7
          docker pull alpine
          docker ps -a -q | xargs -r docker rm
          docker images -q -f dangling=true | xargs -r docker rmi
          docker images | grep -v -P 'alpine|ubuntu|centos|node' | perl -nE 'BEGIN{my @a} unless (/minutes/ || /hours/ || /IMAGE/) {push @a, (split)[2]} END {say for reverse @a}' | xargs -r docker rmi
        '''
      } catch (all) {
        println(all)
          currentBuild.result = 'UNSTABLE'
      }
    }
  }
}
// Stop load
    }
  }
}
