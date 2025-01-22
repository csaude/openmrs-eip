# DB SYNC Release Process

1. [Overview](#overview)
2. [Prior To The Release](#future-of-the-release-process)
2. [Future Of The Release Process](#future-of-the-release-process)
3. [Major Or Minor Release](#major-or-minor-release)
4. [Maintenance Release](#maintenance-release)

## Overview

In DB sync we use [semantic versioning](https://semver.org).

## Future Of The Release Process

The current process to tag a release is manual, we should possibly automate the tagging of a release to be done from a
continuous integration tool.

## Prior To The Release

Be sure to examine all outstanding tickets in JIRA for the release version, all tickets assigned to the release should
have been resolved, tested, approved and closed.

## Major Or Minor Release

A major release typically contains several of changes which could be backwards incompatible and can contain changes to
the database model, below are the steps for a major or minor release.

- Ensure all tickets have been closed as mentioned above under [Prior To The Release](#prior-to-the-release) section.
- Make sure that your local master branch is up to date with the upstream repo.
- Create a maintenance branch for the release line, for instance, if the version in master is 1.8.0-SNAPSHOT, then you
  should create a 1.8.x branch off master.
- Checkout the maintenance branch and run the tests with maven to ensure they all pass.
- Change the version number in preparation for the release, in our example the new version number would be 1.8.0.
- Tag the release in git, the tag name should be the version number of the release, from our example, it would be 1.8.0.
- Checkout the new tag in git.
- Build and run the tag.
- Perform rudimentary testing of the tag on your machine, we suggest this testing to be such that you create a patient
  in your dev remote OpenMRS instance from the UI, and verify from the UI that the patient is fully synchronized to your
  dev central OpenMRS instance.
- Checkout the maintenance branch and change the version number in preparation for the next development iteration, in
  our example the new version number would be 1.8.1-SNAPSHOT.
- Checkout master and change the version number in preparation for the next development iteration, in our example the
  new version number would be 1.9.0-SNAPSHOT.
- Be sure to push to the upstream repo the release tag and all the local changes in the master and the maintenance
  branches.
- Release the version in JIRA, please refer to the JIRA documentation for more details.
- Prepare the release notes in JIRA and add them in master under the [release notes](distribution/docs/release-notes)
  directory, please refer to the JIRA documentation for more details on how to prepare release notes.
- Send out a release notification email to other team members and other interested parties.

## Maintenance Release

A maintenance release contains bug fixes and security patches for use between major releases and usually should not
contain changes to the database model, e.g. going from 1.8.0 to 1.8.1. There is no new maintenance branch to create. You
just tag from the maintenance branch of the minor release that contains the new changes which in our example would be
the 1.8.x branch, below are the steps for a maintenance release.

- Ensure all tickets have been closed as mentioned above under [Prior To The Release](#prior-to-the-release) section.
- Make sure that your local maintenance branch is up to date with the upstream repo.
- Checkout the maintenance branch and run the tests with maven to ensure they all pass.
- Change the version number in preparation for the release, in our example the new version number would be 1.8.1.
- Tag the release in git, the tag name should be the version number of the release.
- Checkout the new tag in git.
- Build and run the tag.
- Perform rudimentary testing of the tag on your machine, we suggest this testing to be such that you create a patient
  in your dev remote OpenMRS instance from the UI, and verify from the UI that the patient is fully synchronized to your
  dev central OpenMRS instance.
- Checkout the maintenance branch and change the version number in preparation for the next development iteration, in
  our example the new version number would be 1.8.2-SNAPSHOT.
- Be sure to push to the upstream repo the release tag and all the local changes in the maintenance branch.
- Release the version in JIRA, please refer to the JIRA documentation for more details.
- Prepare the release notes in JIRA and add them in master under the [release notes](distribution/docs/release-notes)
  directory, please refer to the JIRA documentation for more details on how to prepare release notes.
- Send out a release notification email to other team members and other interested parties.

