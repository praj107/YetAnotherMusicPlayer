import jenkins.model.Jenkins
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import hudson.model.BooleanParameterDefinition
import hudson.model.ChoiceParameterDefinition
import hudson.model.ParametersDefinitionProperty
import hudson.model.StringParameterDefinition
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

def repoUrl = 'https://github.com/praj107/YetAnotherMusicPlayer.git'
def branchSpec = '*/main'

def jobDefinitions = [
    [
        name: 'YetAnotherMusicPlayer-CI',
        scriptPath: 'Jenkinsfile',
        description: 'Continuous verification pipeline for unit tests, lint, and debug assembly.',
        parameters: [
            [type: 'boolean', name: 'RUN_LINT', defaultValue: true, description: 'Run Android lint as part of the quality gate.'],
            [type: 'string', name: 'ANDROID_SDK_ROOT_OVERRIDE', defaultValue: '', description: 'Optional SDK path override. Leave empty to use controller or agent environment variables.']
        ]
    ],
    [
        name: 'YetAnotherMusicPlayer-Connected',
        scriptPath: 'ci/jenkins/pipelines/connected.Jenkinsfile',
        description: 'Device or emulator validation pipeline for connected Android instrumentation tests.',
        parameters: [
            [type: 'boolean', name: 'RUN_UNIT_TESTS', defaultValue: true, description: 'Run JVM unit tests before connected-device validation.'],
            [type: 'string', name: 'ANDROID_SDK_ROOT_OVERRIDE', defaultValue: '', description: 'Optional SDK path override. Leave empty to use controller or agent environment variables.']
        ]
    ],
    [
        name: 'YetAnotherMusicPlayer-Release',
        scriptPath: 'ci/jenkins/pipelines/release.Jenkinsfile',
        description: 'Signed release pipeline that tags, pushes, and publishes GitHub releases.',
        parameters: [
            [type: 'choice', name: 'RELEASE_TYPE', choices: ['patch', 'minor', 'major', 'chore'], description: 'Semantic bump to apply before packaging and publishing. chore republishes the current version.'],
            [type: 'boolean', name: 'RUN_LINT', defaultValue: true, description: 'Run Android lint as part of release validation.'],
            [type: 'boolean', name: 'RUN_CONNECTED_TESTS', defaultValue: false, description: 'Run connected Android tests when an emulator or device is available on the agent.'],
            [type: 'string', name: 'ANDROID_SDK_ROOT_OVERRIDE', defaultValue: '', description: 'Optional SDK path override. Leave empty to use controller or agent environment variables.']
        ]
    ]
]

def buildScm = {
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

def buildParameters = { definitions ->
    definitions.collect { definition ->
        switch (definition.type) {
            case 'boolean':
                return new BooleanParameterDefinition(
                    definition.name,
                    definition.defaultValue,
                    definition.description
                )
            case 'string':
                return new StringParameterDefinition(
                    definition.name,
                    definition.defaultValue,
                    definition.description
                )
            case 'choice':
                return new ChoiceParameterDefinition(
                    definition.name,
                    definition.choices.join('\n'),
                    definition.description
                )
            default:
                throw new IllegalArgumentException("Unsupported parameter type: ${definition.type}")
        }
    }
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
    job.removeProperty(ParametersDefinitionProperty)
    job.addProperty(new ParametersDefinitionProperty(buildParameters(definition.parameters)))
    job.save()
}

println 'bootstrap-complete'
