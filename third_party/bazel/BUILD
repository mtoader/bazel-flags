load(":versions.bzl", "BAZEL_VERSIONS")

NAMES = [{
    "v": version,
    "bazel-repository": "@bazel_%s//file" % version,
    "patch": "//third_party/bazel/%s:export-flags.patch" % version,
    "bazel-dev-jar": "bazel-dev-%s" % version,
    "flags-file": "KnownFlags_%s" % version,
} for version in BAZEL_VERSIONS]

[genrule(
    name = "build-%s" % n["bazel-dev-jar"],
    srcs = [
        n["bazel-repository"],
        n["patch"],
    ],
    outs = [ "%s.jar" % n["bazel-dev-jar"] ],
    tags = [ "manual" ],
    cmd = """
        # Create a temporary directory
        mkdir staged_bazel_repo

        # ls -alR
        # Copy all files from the http_archive
        tar -xzf $(location {repository_file}) -C staged_bazel_repo --strip-components=1

        pushd staged_bazel_repo
        patch -p1 < ../$(location {patch})
        bazel build  --disk_cache=$$TMPDIR/staging_bazel_cache/ //src:bazel-dev
        popd

        # Repackage the modified directory
        cp staged_bazel_repo/bazel-bin/src/bazel-dev $@
    """.format(repository_file = n["bazel-repository"], patch = n["patch"]),
) for n in NAMES]

[genrule(
    name = "build-%s" % n["flags-file"],
    srcs = [
        "%s.jar" % n["bazel-dev-jar"],
    ],
    tools = [
        "//src/main/kotlin:generator",
        "@bazel_tools//tools/jdk:current_java_runtime"
    ],
    outs = [ "%s.kt" % n["flags-file"] ],
    tags = [ "manual" ],
    cmd = """
        $(location //src/main/kotlin:generator) \
            --bazel-binary=$(location {bazel_dev_jar}.jar) \
            --target=known_flags.kt

        cp known_flags.kt $@
    """.format(bazel_dev_jar = n["bazel-dev-jar"]),
) for n in NAMES]

filegroup(
   name = "known_flags",
   srcs = ["%s.kt" % n["flags-file"] for n in NAMES],
)