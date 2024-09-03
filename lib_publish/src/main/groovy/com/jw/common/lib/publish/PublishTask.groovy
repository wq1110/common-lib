package com.jw.common.lib.publish
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PublishTask extends DefaultTask {
    PublishConfig config

    PublishTask() {
        group = '发布'
        setDepends()
    }

    void setDepends() {
        project.afterEvaluate {
            def pubTask = project.tasks.findByName("publishMinePublicationToMavenRepository")
            pubTask.doFirst {
                println("${project.name} 开始发布...")
            }
            if (pubTask != null) {
                dependsOn(pubTask)
            }else
                setDepends()
        }
    }

    @TaskAction
    void publish() {
        println("${project.name} 发布完成...")
    }
}