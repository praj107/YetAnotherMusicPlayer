import jenkins.model.Jenkins
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

def repoUrl = 'https://github.com/praj107/YetAnotherMusicPlayer.git'
def branchSpec = '*/main'

def jobDefinitions = [
    [
        name: 'YetAnotherMusicPlayer-CI',
        scriptPath: 'Jenkinsfile',
        description: 'Continuous verification pipeline for unit tests, lint, and debug assembly.'
    ],
    [
        name: 'YetAnotherMusicPlayer-Connected',
        scriptPath: 'ci/jenkins/pipelines/connected.Jenkinsfile',
        description: 'Device or emulator validation pipeline for connected Android instrumentation tests.'
    ],
    [
        name: 'YetAnotherMusicPlayer-Release',
        scriptPath: 'ci/jenkins/pipelines/release.Jenkinsfile',
        description: 'Signed release pipeline that tags, pushes, and publishes GitHub releases.'
    ]
]

def buildScm() {
    new GitSCM(
        [new UserRemoteConfig(repoUrl, null, null, null)],
        [new BranchSpec(branchSpec)],
        false,
        [],
        null,
        null,
        []
    )
}

def jenkins = Jenkins.instance
jobDefinitions.each { definition ->
    WorkflowJob job = jenkins.getItemByFullName(definition.name, WorkflowJob)
    if (job == null) {
        job = jenkins.createProject(WorkflowJob, definition.name)
        println "created:${definition.name}"
    } else {
        println "updated:${definition.name}"
    }

    def flowDefinition = new CpsScmFlowDefinition(buildScm(), definition.scriptPath)
    flowDefinition.setLightweight(true)
    job.setDefinition(flowDefinition)
    job.setDescription(definition.description)
    job.setDisabled(false)
    job.save()
}

println 'bootstrap-complete'
