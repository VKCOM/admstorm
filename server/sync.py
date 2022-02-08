#!/usr/bin/env python3
import json
import subprocess
import sys
from dataclasses import dataclass
from os.path import isfile
from sys import argv
from typing import Tuple, List

FILE_SEPARATOR = "<<<--->>>"
PART_SEPARATOR = "<--->"
SUBJECT_BODY_SEPARATOR = "<<!>>"
OK = 0
ERROR = 1
FOUND_MISMATCH = 1
DEFAULT_ENCODING = "windows-1251"


class CommandRunner:
    @staticmethod
    def only_stdout(args: str) -> str:
        """
        Runs a command on the server and returns stdout

        :param args: command args
        :return: stdout
        """

        process = subprocess.Popen(args.split(" "), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return process.communicate()[0].decode("utf-8")

    @staticmethod
    def only_stdout_list(args: List[str]) -> str:
        """
        Runs a command on the server and returns stdout

        :param args: command args
        :return: stdout
        """

        process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return process.communicate()[0].decode("utf-8")

    @staticmethod
    def run(args: str) -> Tuple[str, str]:
        """
        Runs a command on the server and returns the output from stdout and stderr in utf-8

        :param args: command args
        :return: tuple with stdout and stderr
        """

        process = subprocess.Popen(args.split(" "), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
        return stdout.decode("utf-8"), stderr.decode("utf-8")


class FileUtils:
    @staticmethod
    def content(path: str, encoding: str) -> str:
        """
        Returns the contents of the file

        :param path: path to the file
        :param encoding: file encoding
        :return: contents of the file
        """

        try:
            with open(path, encoding=encoding) as file:
                return file.read()
        except FileNotFoundError:
            return ""
        except UnicodeEncodeError:
            if encoding != "utf-8":
                return FileUtils.content(path, "utf-8")
            else:
                return "unknown encoding"

    @staticmethod
    def is_exists(filepath: str) -> bool:
        """
        Checks if a file exists
        :param filepath: path to the file
        :return: true if file exists
        """

        return isfile(filepath)

    @staticmethod
    def encoding(filepath: str) -> str:
        """
        Returns the encoding for the passed file
        :param filepath: path to the file
        :return: encoding of file
        """

        encoding = CommandRunner.only_stdout_list(['file', '--mime-encoding', '-b', filepath]).strip()

        # file utility incorrectly detects 8-bit encodings and considers
        # the file to be plain Latin-1, although it is not, so just change
        # to windows-125
        if encoding == "iso-8859-1":
            encoding = "windows-1251"

        # by default
        if encoding == "unknown-8bit":
            encoding = "windows-1251"

        return encoding

    @staticmethod
    def md5(filepath: str) -> str:
        """
        Calculates the md5 hash for the passed file
        :param filepath: path to the file
        :return: md5 hash for the file
        """

        md5_output = CommandRunner.only_stdout_list(['md5sum', filepath]).strip()
        md5 = md5_output.split("  ")[0]
        return md5


@dataclass
class StatusRecord:
    """
    StatusRecord describes one entry from git status
    """

    index: str
    workTree: str
    path: str

    # not empty if the file has been renamed
    origPath: str

    def is_removed(self):
        return self.index == 'D' or self.workTree == 'D'

    def is_renamed(self):
        return self.origPath != ""


def parse_git_status(git_status_output) -> List[StatusRecord]:
    """
    Parses the passed output of the `git status --porcelain -z` command
    into the StatusRecord list.

    :param git_status_output: output of the `git status --porcelain -z` command
    :return: list of files in StatusRecord format
    """

    def is_known_status(status: str) -> bool:
        """
        An additional function to determine if the passed status is known
        :param status: one character string
        :return: true if status is known
        """

        return status == ' ' or status == 'M' or status == 'A' or \
               status == 'D' or status == 'C' or status == 'R' or \
               status == 'U' or status == 'T' or status == '!' or \
               status == '?'

    def is_renamed(status: str) -> bool:
        """
        An additional function that checks if the current record is about
        the file that has been renamed.
        :param status: one character string
        :return: true if status is renamed
        """

        return status == 'R' or status == 'C'

    parts = git_status_output.split("\0")

    need_pass_next_part = False
    files = []
    for idx, part in enumerate(parts):
        if need_pass_next_part:
            need_pass_next_part = False
            continue

        if len(part.strip()) == 0:
            continue
        if part.startswith("starting fsmonitor-daemon in "):
            continue

        # X, Y, space and at least one symbol for the file
        if len(part) < 4 or part[2] != ' ':
            continue

        x_status = part[0]
        y_status = part[1]
        if not is_known_status(x_status) or not is_known_status(y_status):
            continue

        # skipping the space
        path_part = part[3:]
        if is_renamed(x_status) or is_renamed(y_status):
            if idx == len(parts) - 1:
                continue

            # read the "from" filepath which is separated also by NUL character
            orig_path = parts[idx + 1]
            # skip the next part, since it has already been processed
            need_pass_next_part = True

            files.append(StatusRecord(x_status, y_status, path_part, orig_path))
        else:
            files.append(StatusRecord(x_status, y_status, path_part, ""))

    return files


@dataclass
class LocalGitStatusFile:
    """
    Class representing a single local git status file
    """

    path: str
    orig_path: str
    encoding: str
    md5sum: str
    is_renamed: str
    is_conflicted: bool
    is_untracked: bool
    is_ignored: bool
    is_removed: bool

    remote_file_content: str


@dataclass
class RemoteFile:
    """
    Class representing a single remote file
    """

    path: str
    orig_path: str
    encoding: str
    md5: str
    content: str

    def __str__(self):
        return f"{self.path}{PART_SEPARATOR}{self.orig_path}{PART_SEPARATOR}" \
               f"{self.encoding}{PART_SEPARATOR}{self.md5}{PART_SEPARATOR}" \
               f"{self.content}"


@dataclass
class Commit:
    """
    Class representing a single git commit
    """

    hash: str

    author: str
    authorDate: int
    authorEmail: str

    committer: str
    committerDate: int
    committerEmail: str

    subject: str
    body: str

    def __str__(self):
        return f"{self.hash};{self.author};{self.authorDate};{self.authorEmail};" \
               f"{self.committer};{self.committerDate};{self.committerEmail}{SUBJECT_BODY_SEPARATOR}" \
               f"{self.subject}{SUBJECT_BODY_SEPARATOR}{self.body}"

    @staticmethod
    def get(commit_hash: str):
        """
        Gets the commit object by hash
        :param commit_hash: commit hash
        :return: Commit object or None if commit not found
        """

        # Hash
        # Author
        # Author Date Unix
        # Author Email
        # Committer
        # Committer Date Unix
        # Committer Email
        # Subject
        # Body
        output_format = f"%H;%an;%at;%aE;%cn;%ct;%cE{SUBJECT_BODY_SEPARATOR}%s{SUBJECT_BODY_SEPARATOR}%b"

        stdout = CommandRunner.only_stdout(f"git log -1 --format={output_format} {commit_hash}")

        parts = stdout.split(SUBJECT_BODY_SEPARATOR)
        if len(parts) != 3:
            return None

        main_parts = parts[0].split(";")
        if len(main_parts) != 7:
            return None

        subject = parts[1]
        body = parts[2]

        return Commit(
            main_parts[0],
            main_parts[1],
            int(main_parts[2]),
            main_parts[3],
            main_parts[4],
            int(main_parts[5]),
            main_parts[6],
            subject,
            body
        )

    def distance(self, other) -> int:
        """
        Calculates the distance with the other commit

        :param other: other commit object
        :return: distance between self and other
        """

        hash_range = f"{self.hash}..{other.hash}"
        if other.committerDate < self.committerDate:
            hash_range = f"{other.hash}..{self.hash}"

        stdout = CommandRunner.only_stdout(f"git rev-list --count {hash_range}")

        try:
            res = int(stdout) - 1
        except ValueError:
            return -1

        return res


def git_status_files_from_json(local_git_status_json_data) -> List[LocalGitStatusFile]:
    """
    Converts an associative array obtained from local_git_status_json_data
    to an array of GitStatusFile objects.
    :param local_git_status_json_data: json string describing data
    :return: list of GitStatusFile
    """

    local_git_status_files_raw = json.loads(local_git_status_json_data)
    local_git_status_files = []

    for file in local_git_status_files_raw:
        local_git_status_files.append(
            LocalGitStatusFile(
                file['path'],
                file['origPath'],
                file['encoding'],
                file['md5sum'],
                file['isRenamed'],
                file['isConflicted'],
                file['isUntracked'],
                file['isIgnored'],
                file['isRemoved'],

                "",
            )
        )

    return local_git_status_files


def handle_files_mismatch(diff_files: List[LocalGitStatusFile], undef_files: List[LocalGitStatusFile]) -> None:
    """
    Processes found differences in files

    :param diff_files: Found files that differ
    :param undef_files: Found files that are missing on the server
    """

    print("files mismatch")

    mismatch_files = []

    for idx, local_file in enumerate(undef_files):
        orig_path = ""
        # if the file was renamed, then we send back the original path
        if local_file.remote_file_content == "<renamed>":
            orig_path = local_file.orig_path

        remote_file = RemoteFile(
            local_file.path,
            orig_path,
            DEFAULT_ENCODING,
            "",
            local_file.remote_file_content
        )
        mismatch_files.append(remote_file)

    for idx, local_file in enumerate(diff_files):
        # if the encoding is not set, then it is most likely a removed file,
        # so we cannot find out its encoding locally, so here we resolve it
        # by the file encoding on the server
        #
        # this is necessary to read the file correctly further
        if local_file.encoding == "":
            local_file.encoding = FileUtils.encoding(local_file.path)

        content = FileUtils.content(local_file.path, local_file.encoding)

        remote_file = RemoteFile(
            local_file.path,
            "",
            local_file.encoding,
            # we don't calculate md5 for the file, since we do not need to
            # compare the equivalence with the local file for it
            "",
            content
        )

        mismatch_files.append(remote_file)

    for idx, remote_file in enumerate(mismatch_files):
        print(remote_file, end="")
        if idx != len(mismatch_files) - 1:
            print(FILE_SEPARATOR, end="")


def process_git_status() -> int:
    """
    Parses the current git status and prints all files from it to stdout

    For each file, file path, contents, md5, encoding and original file
    path (if the file was renamed) are displayed.
    :return:
    """

    git_status = CommandRunner.only_stdout("git status --porcelain -z -uno")
    files = parse_git_status(git_status)

    for idx, file in enumerate(files):
        if file.is_removed():
            remote_file = RemoteFile(file.path, "", "", "", "<removed>")
        else:
            orig_path = file.origPath
            encoding = FileUtils.encoding(file.path)
            md5 = FileUtils.md5(file.path)
            content = FileUtils.content(file.path, encoding)

            remote_file = RemoteFile(file.path, orig_path, encoding, md5, content)

        print(remote_file, end="")

        if idx != len(files) - 1:
            print(FILE_SEPARATOR, end="")

    return 0


def cmd_print_files(files_key_value: str) -> int:
    """
    Processes the 'files_content' command
    Prints files passed in files_key_value

    :param files_key_value: files to print
    """

    # format: file_path:encoding[;file_path_1:encoding_1]...
    files = list(map(lambda file: file.split(":"), files_key_value.split(";")))

    for idx, (path, encoding) in enumerate(files):
        if encoding == "":
            encoding = None

        content = FileUtils.content(path, encoding)
        if content == "":
            continue

        print(content, end='')

        if idx != len(files) - 1:
            print(FILE_SEPARATOR, end="")

    return 0


def main() -> int:
    if argv[1] == "files_content":
        return cmd_print_files(argv[2])

    local_branch = argv[1]
    local_hash = argv[2]
    local_git_status_json_data = argv[3]

    cur_branch = CommandRunner.only_stdout("git rev-parse --abbrev-ref HEAD").strip()

    # first stage: branches
    if local_branch != cur_branch:
        print(f"branch mismatch{PART_SEPARATOR}{cur_branch}", end="")

        return FOUND_MISMATCH

    cur_commit_hash = CommandRunner.only_stdout("git rev-parse HEAD").strip()

    # second stage: commits
    if local_hash != cur_commit_hash:
        local_commit = Commit.get(local_hash)
        cur_commit = Commit.get(cur_commit_hash)

        # if local_commit is None then it was not found on the server, which means that
        # it is newer and only located locally, so we return -2 for the plugin to
        # resolve it locally
        distance = -2 if local_commit is None else local_commit.distance(cur_commit)

        print(f"commit mismatch{PART_SEPARATOR}"
              f"{distance}{PART_SEPARATOR}"
              f"{cur_commit}{PART_SEPARATOR}", end="")

        return FOUND_MISMATCH

    # third stage: files
    local_git_status_files = git_status_files_from_json(local_git_status_json_data)

    different_files = []
    undefined_files = []

    for local_file in local_git_status_files:
        if local_file.is_removed:
            if FileUtils.is_exists(local_file.path):
                # file was deleted locally but exists on the server.
                different_files.append(local_file)
            continue

        if not FileUtils.is_exists(local_file.path):
            local_file.remote_file_content = "<not-found>"

            if local_file.is_renamed:
                local_file.remote_file_content = "<renamed>"

            undefined_files.append(local_file)
            continue

        md5 = FileUtils.md5(local_file.path)
        local_md5 = local_file.md5sum

        if md5 != local_md5:
            different_files.append(local_file)

    # if no difference is found, then we process the
    # server git status and display all files from it
    if len(different_files) == 0 and len(undefined_files) == 0:
        return process_git_status()

    handle_files_mismatch(different_files, undefined_files)
    print(FILE_SEPARATOR, end="")

    process_git_status()

    return FOUND_MISMATCH


if __name__ == '__main__':
    sys.exit(main())
