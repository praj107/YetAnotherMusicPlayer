#!/usr/bin/env python3
from __future__ import annotations

import argparse
import base64
import json
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib import error, parse, request


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ENV_FILE = PROJECT_ROOT / ".env"


class JenkinsError(RuntimeError):
    pass


@dataclass
class JenkinsConfig:
    url: str
    username: str
    token: str
    timeout: int


def load_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def job_path(job_name: str) -> str:
    parts = [part for part in job_name.split("/") if part]
    if not parts:
        raise JenkinsError("job name cannot be empty")
    return "".join(f"/job/{parse.quote(part, safe='')}" for part in parts)


class JenkinsClient:
    def __init__(self, config: JenkinsConfig):
        self.base_url = config.url.rstrip("/")
        self.timeout = config.timeout
        self.username = config.username
        token = base64.b64encode(f"{config.username}:{config.token}".encode("utf-8")).decode("ascii")
        self.auth_header = f"Basic {token}"
        self._crumb: tuple[str, str] | None = None

    def _request(
        self,
        path: str,
        *,
        method: str = "GET",
        params: dict[str, Any] | None = None,
        data: bytes | None = None,
        accept: str | None = "application/json",
        content_type: str | None = None,
        use_crumb: bool = False,
    ) -> tuple[int, dict[str, str], bytes]:
        if path.startswith("http://") or path.startswith("https://"):
            url = path
        else:
            path = path if path.startswith("/") else f"/{path}"
            url = f"{self.base_url}{path}"
        if params:
            query = parse.urlencode(params, doseq=True)
            separator = "&" if "?" in url else "?"
            url = f"{url}{separator}{query}"

        headers = {"Authorization": self.auth_header}
        if accept:
            headers["Accept"] = accept
        if content_type:
            headers["Content-Type"] = content_type
        if use_crumb:
            crumb_field, crumb_value = self.crumb()
            headers[crumb_field] = crumb_value

        req = request.Request(url, data=data, headers=headers, method=method)
        try:
            with request.urlopen(req, timeout=self.timeout) as response:
                return response.status, dict(response.headers.items()), response.read()
        except error.HTTPError as exc:
            body = exc.read().decode("utf-8", "replace").strip()
            message = f"{method} {url} failed with HTTP {exc.code}"
            if body:
                snippet = body.replace("\n", " ")
                message = f"{message}: {snippet[:500]}"
            raise JenkinsError(message) from exc
        except error.URLError as exc:
            raise JenkinsError(f"{method} {url} failed: {exc.reason}") from exc

    def _json(
        self,
        path: str,
        *,
        method: str = "GET",
        params: dict[str, Any] | None = None,
        data: bytes | None = None,
        content_type: str | None = None,
        use_crumb: bool = False,
    ) -> dict[str, Any]:
        _, _, body = self._request(
            path,
            method=method,
            params=params,
            data=data,
            content_type=content_type,
            use_crumb=use_crumb,
        )
        return json.loads(body.decode("utf-8"))

    def _text(
        self,
        path: str,
        *,
        method: str = "GET",
        params: dict[str, Any] | None = None,
        data: bytes | None = None,
        content_type: str | None = None,
        use_crumb: bool = False,
        accept: str = "text/plain",
    ) -> str:
        _, _, body = self._request(
            path,
            method=method,
            params=params,
            data=data,
            accept=accept,
            content_type=content_type,
            use_crumb=use_crumb,
        )
        return body.decode("utf-8", "replace")

    def crumb(self) -> tuple[str, str]:
        if self._crumb is None:
            crumb_info = self._json("/crumbIssuer/api/json")
            self._crumb = (crumb_info["crumbRequestField"], crumb_info["crumb"])
        return self._crumb

    def login_info(self) -> dict[str, Any]:
        who = self._json("/whoAmI/api/json")
        meta = self._json("/api/json", params={"tree": "quietingDown,useCrumbs,nodeDescription"})
        _, headers, _ = self._request("/login", method="HEAD", accept=None)
        user_id = who.get("id") or who.get("name") or self.username
        return {
            "id": user_id,
            "authenticated": who.get("authenticated"),
            "anonymous": who.get("anonymous"),
            "authorities": who.get("authorities", []),
            "jenkinsVersion": headers.get("X-Jenkins"),
            "nodeDescription": meta.get("nodeDescription"),
            "useCrumbs": meta.get("useCrumbs"),
            "quietingDown": meta.get("quietingDown"),
        }

    def list_jobs(self) -> list[dict[str, Any]]:
        data = self._json("/api/json", params={"tree": "jobs[name,fullName,url,color,_class]"})
        return data.get("jobs", [])

    def queue(self) -> list[dict[str, Any]]:
        data = self._json(
            "/queue/api/json",
            params={
                "tree": "items[id,task[name,url],why,blocked,stuck,buildable,params,inQueueSince,executable[number,url]]"
            },
        )
        return data.get("items", [])

    def _parameter_definitions(self, properties: list[dict[str, Any]]) -> list[dict[str, Any]]:
        definitions: list[dict[str, Any]] = []
        for item in properties or []:
            for parameter in item.get("parameterDefinitions", []):
                default_value = parameter.get("defaultParameterValue", {}).get("value")
                definitions.append(
                    {
                        "name": parameter.get("name"),
                        "type": parameter.get("_class", ""),
                        "default": default_value,
                        "choices": parameter.get("choices", []),
                    }
                )
        return definitions

    def job_info(self, job_name: str) -> dict[str, Any]:
        tree = (
            "name,fullName,url,color,inQueue,buildable,nextBuildNumber,"
            "lastBuild[number,result,building,displayName,url,timestamp,duration],"
            "lastCompletedBuild[number,result,url,timestamp,duration],"
            "lastSuccessfulBuild[number,result,url,timestamp,duration],"
            "lastFailedBuild[number,result,url,timestamp,duration],"
            "builds[number,result,building,displayName,url,timestamp,duration],"
            "property[parameterDefinitions[name,_class,defaultParameterValue[value],choices]]"
        )
        data = self._json(f"{job_path(job_name)}/api/json", params={"tree": tree})
        data["parameters"] = self._parameter_definitions(data.get("property", []))
        data["builds"] = data.get("builds", [])
        return data

    def build_info(self, job_name: str, build_number: int | str) -> dict[str, Any]:
        tree = (
            "number,result,building,displayName,fullDisplayName,url,timestamp,duration,description,"
            "estimatedDuration,artifacts[fileName,relativePath],actions[parameters[name,value]]"
        )
        return self._json(f"{job_path(job_name)}/{build_number}/api/json", params={"tree": tree})

    def build_console(self, job_name: str, build_number: int | str) -> str:
        return self._text(f"{job_path(job_name)}/{build_number}/consoleText")

    def _normalize_parameter_defaults(self, definitions: list[dict[str, Any]]) -> dict[str, str]:
        defaults: dict[str, str] = {}
        for definition in definitions:
            name = definition.get("name")
            if not name:
                continue
            value = definition.get("default")
            if value is None:
                continue
            defaults[name] = str(value).lower() if isinstance(value, bool) else str(value)
        return defaults

    def trigger_build(self, job_name: str, parameters: dict[str, str]) -> dict[str, Any]:
        info = self.job_info(job_name)
        definitions = info.get("parameters", [])
        is_parameterized = bool(definitions)
        payload = dict(parameters)
        path = f"{job_path(job_name)}/build"
        body = b""
        content_type = None
        if is_parameterized:
            path = f"{job_path(job_name)}/buildWithParameters"
            defaults = self._normalize_parameter_defaults(definitions)
            defaults.update(payload)
            payload = defaults
            body = parse.urlencode(payload).encode("utf-8")
            content_type = "application/x-www-form-urlencoded"
        elif payload:
            raise JenkinsError(f"job '{job_name}' is not parameterized")

        _, headers, _ = self._request(
            path,
            method="POST",
            data=body,
            content_type=content_type,
            use_crumb=True,
            accept=None,
        )
        queue_url = headers.get("Location")
        if not queue_url:
            raise JenkinsError("trigger succeeded but Jenkins did not return a queue location")
        queue_path = queue_url.replace(self.base_url, "")
        return {
            "job": job_name,
            "parameters": payload,
            "queueUrl": queue_url,
            "queuePath": queue_path,
            "nextBuildNumber": info.get("nextBuildNumber"),
        }

    def wait_for_queue(self, queue_path: str, timeout_seconds: int, poll_seconds: float) -> dict[str, Any]:
        deadline = time.time() + timeout_seconds
        while time.time() < deadline:
            queue_info = self._json(f"{queue_path}/api/json")
            executable = queue_info.get("executable")
            if executable and executable.get("number") is not None:
                return queue_info
            if queue_info.get("cancelled"):
                raise JenkinsError(f"queue item {queue_info.get('id')} was cancelled")
            time.sleep(poll_seconds)
        raise JenkinsError("timed out waiting for queued build to start")

    def wait_for_build(
        self, job_name: str, build_number: int | str, timeout_seconds: int, poll_seconds: float
    ) -> dict[str, Any]:
        deadline = time.time() + timeout_seconds
        while time.time() < deadline:
            info = self.build_info(job_name, build_number)
            if not info.get("building"):
                return info
            time.sleep(poll_seconds)
        raise JenkinsError(f"timed out waiting for build {build_number} to finish")

    def progressive_console(
        self, job_name: str, build_number: int | str, start: int = 0
    ) -> tuple[str, int, bool]:
        _, headers, body = self._request(
            f"{job_path(job_name)}/{build_number}/logText/progressiveText",
            params={"start": start},
            accept="text/plain",
        )
        text = body.decode("utf-8", "replace")
        next_start = int(headers.get("X-Text-Size", start))
        more_data = headers.get("X-More-Data", "false").lower() == "true"
        return text, next_start, more_data

    def abort_build(self, job_name: str, build_number: int | str) -> dict[str, Any]:
        self._request(f"{job_path(job_name)}/{build_number}/stop", method="POST", data=b"", use_crumb=True, accept=None)
        return {"job": job_name, "build": int(build_number), "status": "stop-requested"}

    def run_script(self, script: str) -> str:
        body = parse.urlencode({"script": script}).encode("utf-8")
        return self._text(
            "/scriptText",
            method="POST",
            data=body,
            content_type="application/x-www-form-urlencoded",
            use_crumb=True,
        )


def parse_parameters(parameter_items: list[str]) -> dict[str, str]:
    parameters: dict[str, str] = {}
    for item in parameter_items:
        if "=" not in item:
            raise JenkinsError(f"invalid parameter '{item}', expected KEY=VALUE")
        key, value = item.split("=", 1)
        parameters[key] = value
    return parameters


def print_json(data: Any) -> None:
    print(json.dumps(data, indent=2, sort_keys=True))


def render_login_info(info: dict[str, Any]) -> None:
    print(f"User: {info.get('id')}")
    print(f"Authenticated: {info.get('authenticated')}")
    print(f"Jenkins: {info.get('jenkinsVersion')}")
    print(f"Node: {info.get('nodeDescription')}")
    print(f"Crumbs: {info.get('useCrumbs')}")
    print(f"Quieting Down: {info.get('quietingDown')}")


def render_jobs(jobs: list[dict[str, Any]]) -> None:
    if not jobs:
        print("No jobs found.")
        return
    for job in jobs:
        print(f"{job.get('fullName', job.get('name'))}\t{job.get('color')}\t{job.get('url')}")


def render_job_info(info: dict[str, Any], limit: int) -> None:
    print(f"Job: {info.get('fullName', info.get('name'))}")
    print(f"URL: {info.get('url')}")
    print(f"Color: {info.get('color')}")
    print(f"Buildable: {info.get('buildable')}")
    print(f"In Queue: {info.get('inQueue')}")
    print(f"Next Build: {info.get('nextBuildNumber')}")
    if info.get("parameters"):
        print("Parameters:")
        for parameter in info["parameters"]:
            line = f"  - {parameter['name']} default={parameter.get('default')!r}"
            if parameter.get("choices"):
                line += f" choices={parameter['choices']}"
            print(line)
    last_build = info.get("lastBuild")
    if last_build:
        print(
            f"Last Build: #{last_build.get('number')} {last_build.get('result')} "
            f"building={last_build.get('building')} url={last_build.get('url')}"
        )
    builds = info.get("builds", [])[:limit]
    if builds:
        print("Recent Builds:")
        for build in builds:
            print(
                f"  - #{build.get('number')} {build.get('result')} "
                f"building={build.get('building')} {build.get('url')}"
            )


def render_builds(builds: list[dict[str, Any]]) -> None:
    if not builds:
        print("No builds found.")
        return
    for build in builds:
        print(
            f"#{build.get('number')}\t{build.get('result')}\t"
            f"building={build.get('building')}\t{build.get('url')}"
        )


def render_build_info(info: dict[str, Any]) -> None:
    print(f"Build: #{info.get('number')} {info.get('fullDisplayName', info.get('displayName'))}")
    print(f"Result: {info.get('result')}")
    print(f"Building: {info.get('building')}")
    print(f"URL: {info.get('url')}")
    print(f"Duration: {info.get('duration')}")
    parameters = []
    for action in info.get("actions", []):
        parameters.extend(action.get("parameters", []))
    if parameters:
        print("Parameters:")
        for parameter in parameters:
            print(f"  - {parameter.get('name')}={parameter.get('value')}")
    artifacts = info.get("artifacts", [])
    if artifacts:
        print("Artifacts:")
        for artifact in artifacts:
            print(f"  - {artifact.get('relativePath')}")


def render_queue(items: list[dict[str, Any]]) -> None:
    if not items:
        print("Queue is empty.")
        return
    for item in items:
        executable = item.get("executable") or {}
        print(
            f"id={item.get('id')} task={item.get('task', {}).get('name')} "
            f"blocked={item.get('blocked')} buildable={item.get('buildable')} "
            f"build=#{executable.get('number')} why={item.get('why')}"
        )


def follow_console(client: JenkinsClient, job_name: str, build_number: int, poll_seconds: float) -> dict[str, Any]:
    start = 0
    while True:
        text, start, more_data = client.progressive_console(job_name, build_number, start=start)
        if text:
            sys.stdout.write(text)
            sys.stdout.flush()
        info = client.build_info(job_name, build_number)
        if not info.get("building") and not more_data:
            return info
        time.sleep(poll_seconds)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Repo-owned Jenkins client for YetAnotherMusicPlayer operations.")
    parser.add_argument("--url", help="Jenkins base URL. Defaults to JENKINS_URL or http://127.0.0.1:8080.")
    parser.add_argument("--user", help="Jenkins username. Defaults to JENKINS_USERNAME from the env file.")
    parser.add_argument("--token", help="Jenkins API token or password. Defaults to JENKINS_TOKEN from the env file.")
    parser.add_argument(
        "--env-file",
        default=str(DEFAULT_ENV_FILE),
        help=f"Env file to load credentials from. Defaults to {DEFAULT_ENV_FILE}.",
    )
    parser.add_argument("--timeout", type=int, default=30, help="HTTP request timeout in seconds.")
    parser.add_argument("--json", action="store_true", help="Emit JSON instead of human-readable text.")

    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("login", help="Validate credentials and print controller metadata.")
    subparsers.add_parser("jobs", help="List Jenkins jobs.")
    subparsers.add_parser("queue", help="List queued Jenkins items.")

    job_info_parser = subparsers.add_parser("job-info", help="Show job metadata and recent builds.")
    job_info_parser.add_argument("job")
    job_info_parser.add_argument("--limit", type=int, default=5, help="Number of recent builds to show.")

    builds_parser = subparsers.add_parser("builds", help="List recent builds for a job.")
    builds_parser.add_argument("job")
    builds_parser.add_argument("--limit", type=int, default=10, help="Number of builds to show.")

    build_info_parser = subparsers.add_parser("build-info", help="Show build metadata.")
    build_info_parser.add_argument("job")
    build_info_parser.add_argument("build", type=int)

    console_parser = subparsers.add_parser("console", help="Show or follow build console logs.")
    console_parser.add_argument("job")
    console_parser.add_argument("build", type=int)
    console_parser.add_argument("--tail", type=int, default=200, help="Number of lines to show when not following.")
    console_parser.add_argument("--follow", action="store_true", help="Stream progressive console output.")
    console_parser.add_argument("--poll", type=float, default=2.0, help="Polling interval in seconds when following.")

    trigger_parser = subparsers.add_parser("trigger", help="Trigger a Jenkins job and optionally wait for it.")
    trigger_parser.add_argument("job")
    trigger_parser.add_argument(
        "--param",
        action="append",
        default=[],
        metavar="KEY=VALUE",
        help="Build parameter. Can be provided multiple times.",
    )
    trigger_parser.add_argument("--wait", action="store_true", help="Wait for the build to finish.")
    trigger_parser.add_argument("--follow", action="store_true", help="Follow console output until the build finishes.")
    trigger_parser.add_argument("--poll", type=float, default=2.0, help="Polling interval in seconds.")
    trigger_parser.add_argument("--queue-timeout", type=int, default=300, help="Queue wait timeout in seconds.")
    trigger_parser.add_argument("--build-timeout", type=int, default=3600, help="Build wait timeout in seconds.")

    wait_parser = subparsers.add_parser("wait", help="Wait for a specific build to finish.")
    wait_parser.add_argument("job")
    wait_parser.add_argument("build", type=int)
    wait_parser.add_argument("--poll", type=float, default=2.0, help="Polling interval in seconds.")
    wait_parser.add_argument("--timeout-seconds", type=int, default=3600, help="Build wait timeout in seconds.")

    abort_parser = subparsers.add_parser("abort", help="Request that a running build stop.")
    abort_parser.add_argument("job")
    abort_parser.add_argument("build", type=int)

    script_parser = subparsers.add_parser("script", help="Run Groovy in the Jenkins script console.")
    script_group = script_parser.add_mutually_exclusive_group(required=True)
    script_group.add_argument("--file", help="Path to a Groovy script file.")
    script_group.add_argument("--code", help="Inline Groovy code.")

    return parser


def resolve_config(args: argparse.Namespace) -> JenkinsConfig:
    env_file = Path(args.env_file).expanduser()
    env_values = load_env_file(env_file)
    url = args.url or env_values.get("JENKINS_URL") or "http://127.0.0.1:8080"
    username = args.user or env_values.get("JENKINS_USERNAME")
    token = args.token or env_values.get("JENKINS_TOKEN")
    if not username or not token:
        raise JenkinsError(
            f"Missing Jenkins credentials. Provide --user/--token or populate {env_file} with JENKINS_USERNAME and JENKINS_TOKEN."
        )
    return JenkinsConfig(url=url, username=username, token=token, timeout=args.timeout)


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        client = JenkinsClient(resolve_config(args))

        if args.command == "login":
            info = client.login_info()
            print_json(info) if args.json else render_login_info(info)
            return 0

        if args.command == "jobs":
            jobs = client.list_jobs()
            print_json(jobs) if args.json else render_jobs(jobs)
            return 0

        if args.command == "queue":
            items = client.queue()
            print_json(items) if args.json else render_queue(items)
            return 0

        if args.command == "job-info":
            info = client.job_info(args.job)
            print_json(info) if args.json else render_job_info(info, args.limit)
            return 0

        if args.command == "builds":
            info = client.job_info(args.job)
            builds = info.get("builds", [])[: args.limit]
            print_json(builds) if args.json else render_builds(builds)
            return 0

        if args.command == "build-info":
            info = client.build_info(args.job, args.build)
            print_json(info) if args.json else render_build_info(info)
            return 0

        if args.command == "console":
            if args.follow:
                info = follow_console(client, args.job, args.build, args.poll)
                if args.json:
                    print_json(info)
                else:
                    print(f"\nFinal Result: {info.get('result')}")
                return 0 if info.get("result") == "SUCCESS" else 1
            console_text = client.build_console(args.job, args.build)
            lines = console_text.splitlines()
            if args.tail > 0:
                lines = lines[-args.tail :]
            print("\n".join(lines))
            return 0

        if args.command == "trigger":
            parameters = parse_parameters(args.param)
            trigger = client.trigger_build(args.job, parameters)
            queue_info = client.wait_for_queue(trigger["queuePath"], args.queue_timeout, args.poll)
            build_number = queue_info["executable"]["number"]
            result = {
                "job": args.job,
                "queue": trigger,
                "build": client.build_info(args.job, build_number),
            }
            if args.follow:
                result["build"] = follow_console(client, args.job, build_number, args.poll)
            elif args.wait:
                result["build"] = client.wait_for_build(args.job, build_number, args.build_timeout, args.poll)
            if args.json:
                print_json(result)
            else:
                print(f"Triggered {args.job} build #{build_number}")
                print(f"Queue URL: {trigger['queueUrl']}")
                if args.wait or args.follow:
                    print(f"Final Result: {result['build'].get('result')}")
                else:
                    print(f"Build URL: {result['build'].get('url')}")
            if args.wait or args.follow:
                return 0 if result["build"].get("result") == "SUCCESS" else 1
            return 0

        if args.command == "wait":
            info = client.wait_for_build(args.job, args.build, args.timeout_seconds, args.poll)
            print_json(info) if args.json else render_build_info(info)
            return 0 if info.get("result") == "SUCCESS" else 1

        if args.command == "abort":
            result = client.abort_build(args.job, args.build)
            print_json(result) if args.json else print(
                f"Stop requested for {result['job']} build #{result['build']}"
            )
            return 0

        if args.command == "script":
            script = Path(args.file).read_text(encoding="utf-8") if args.file else args.code
            output = client.run_script(script)
            if args.json:
                print_json({"output": output})
            else:
                print(output, end="" if output.endswith("\n") else "\n")
            return 0

        parser.error(f"unsupported command: {args.command}")
        return 2
    except JenkinsError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
