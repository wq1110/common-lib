package com.jw.common.lib.publish

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

class PublishPlugin implements Plugin<Project> {
    PublishConfigRoot publishConfigRoot
    final String extensionName = "PluginForPublish"

    @Override
    void apply(Project target) {
        if (target == target.getRootProject()) {
            target.extensions.create(extensionName, PublishConfigRoot)
            target.afterEvaluate {
                displayTips()
                publishConfigRoot = target.extensions.getByType(PublishConfigRoot)
                publishConfigRoot.artifactId = ""
                publishConfigRoot.projectFilter = publishConfigRoot.projectFilter == null ? {true} : publishConfigRoot.projectFilter

                checkConfig(publishConfigRoot)

                target.getSubprojects().each {
                    it.extensions.create(extensionName, PublishConfig)
                    it.afterEvaluate { Project subProject ->
                        if (isAppProject(subProject) || !publishConfigRoot.projectFilter.call(subProject)){
                            return
                        }

                        def subConfig = subProject.extensions.getByType(PublishConfig)
                        if (subConfig.artifactId == null || subConfig.artifactId.isEmpty()) {
                            subConfig.artifactId = subProject.name
                        }
                        subConfig.mergeData(publishConfigRoot)
                        printConfig(subConfig, subProject)

                        def setComponentFunc
                        def setSourceCodeFunc
                        if (isJavaLibProject(subProject)) {
                            setComponentFunc = { MavenPublication mavenPublication ->
                                subProject.afterEvaluate {
                                    if (subProject.components.java != null) {
                                        mavenPublication.from {subProject.components.java}
                                    }
                                }
                            }
                            setSourceCodeFunc = { MavenPublication mavenPublication ->
                                def srcTask = subProject.task(["type": Jar.class], "sourcesJar")
                                srcTask.archiveClassifier.set("sources")
                                srcTask.from(subProject.sourceSets.main.allJava.getSrcDirs())
                                mavenPublication.artifact(srcTask)
                            }
                        } else if (isAndroidLibProject(subProject)) {
                            setComponentFunc = { MavenPublication mavenPublication ->
                                subProject.afterEvaluate {
                                    def releaseComponent = subProject.components.find {
                                        it.name.toLowerCase().contains('release')
                                    }
                                    if (releaseComponent != null) {
                                        mavenPublication.from(releaseComponent)
                                    }
                                }
                            }
                            setSourceCodeFunc = { MavenPublication mavenPublication ->
                                def srcTask = subProject.task(["type": Jar.class], "sourcesJar")
                                srcTask.archiveClassifier.set("sources")
                                srcTask.from(subProject.android.sourceSets.main.java.getSourceFiles().getFiles())
                                mavenPublication.artifact(srcTask)
                            }
                        } else
                            return

                        subProject.apply plugin: 'maven-publish'
                        subProject.publishing {
                            publications {
                                mine(MavenPublication) {
                                    setComponentFunc(it)
                                    setSourceCodeFunc(it)
                                    groupId subConfig.groupId
                                    artifactId subConfig.artifactId
                                    version subConfig.getFullVersion()
                                }
                            }

                            repositories {
                                maven {
                                    url subConfig.getRepo()
                                    credentials {
                                        username subConfig.getRepoUsrName()
                                        password subConfig.getRepoPsw()
                                    }
                                }
                            }
                        }

                        subProject.task("type": MavenPublishTask, "publishToRemoteRepo") {
                            it.config = subConfig
                        }
                    }
                }
            }
        } else {
            throw new GradleException("请在根工程使用该插件 子工程:${target.name} 不应该apply PluginForPublish")
        }
    }

    void checkConfig(PublishConfig config) {
        def errorInfo = ""
        config.properties.findAll {
            it.value == null && it.key != 'taskThatOwnsThisObject'
        }.each {
            errorInfo += "\n${extensionName}->${it.key} 没有配置"
        }
        if (!errorInfo.isEmpty()) throw new GradleException(errorInfo)
    }

    void printConfig(PublishConfig config, Project project) {
        println "工程 ${project.name} 打包配置: ${config.groupId}:${config.artifactId}:${config.getFullVersion()} ==>Repo: ${config.getRepo()}"
    }

    void displayTips() {
        println "**************************************************************"
        println "**************************************************************"
        println "******************$extensionName 配置说明******************"
        println "请在根工程 下build.gradle中配置: \n" +
                "$extensionName {\n" +
                "\tisSnapshot value(boolean 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\tgroupId value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\tversion value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\trepoRelease value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\trepoReleaseUser value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\trepoReleasePsw value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\trepoSnapshot value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\trepoSnapshotUser value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "\trepoSnapshotPsw value(String 所有的子工程都会应用, 若要单独控制, 子工程覆写)\n" +
                "}"
        println "子工程 请在子工程 下build.gradle同样配置 比如:"
        println "$extensionName { \n" +
                "\tartifactId value(String 默认为工程名)\n" +
                "\tversion newValue(String 如果有单独的版本控制, 就设置一个新的版本号)\n" +
                "}\n"
        println "**************************************************************"
        println "**************************************************************"
        println "**************************************************************"
    }

    boolean isAppProject(Project project) {
        return project.extensions.findByName("android") != null && project.extensions.findByName("android") instanceof AppExtension
    }

    boolean isJavaLibProject(Project project) {
        return project.extensions.findByName("java") != null && project.extensions.findByName("android") == null
    }

    boolean isAndroidLibProject(Project project) {
        return project.extensions.findByName("android") != null && project.extensions.findByName("android") instanceof LibraryExtension
    }
}