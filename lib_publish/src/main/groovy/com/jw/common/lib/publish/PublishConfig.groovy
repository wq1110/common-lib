package com.jw.common.lib.publish

class PublishConfig implements Cloneable {
    Boolean isSnapshot
    String groupId
    String artifactId
    String version
    String repoRelease
    String repoReleaseUser
    String repoReleasePsw
    String repoSnapshot
    String repoSnapshotUser
    String repoSnapshotPsw

    String getFullVersion() {
        version + (isSnapshot ? "-SNAPSHOT" : "")
    }

    String getRepo() {
        isSnapshot ? repoSnapshot : repoRelease
    }

    String getRepoUser() {
        isSnapshot ? repoSnapshotUser : repoReleaseUser
    }

    String getRepoPsw() {
        isSnapshot ? repoSnapshotPsw : repoReleasePsw
    }

    void mergeData(PublishConfig other) {
        if (other != null) {
            if (isEmpty(groupId)) groupId = other.groupId
            if (isEmpty(version)) version = other.version
            if (isEmpty(repoRelease)) repoRelease = other.repoRelease
            if (isEmpty(repoReleaseUser)) repoReleaseUser = other.repoReleaseUser
            if (isEmpty(repoReleasePsw)) repoReleasePsw = other.repoReleasePsw
            if (isEmpty(repoSnapshot)) repoSnapshot = other.repoSnapshot
            if (isEmpty(repoSnapshotUser)) repoSnapshotUser = other.repoSnapshotUser
            if (isEmpty(repoSnapshotPsw)) repoSnapshotPsw = other.repoSnapshotPsw
            if (isEmpty(isSnapshot)) isSnapshot = other.isSnapshot
        }
    }

    private static boolean isEmpty(Object object) {
        if (object == null || (object instanceof String && object.isEmpty())) {
            return true
        }
        return false
    }
}