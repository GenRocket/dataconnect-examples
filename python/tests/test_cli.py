import argparse
import json

import pytest

from genrocket_dataconnect.cli import run


@pytest.mark.asyncio
async def test_dry_run_does_not_require_configuration(capsys: pytest.CaptureFixture[str]) -> None:
    arguments = argparse.Namespace(
        protocol="rest",
        scenario="explicit",
        env_file=None,
        loop_count=3,
        receiver="json",
        output=None,
        dry_run=True,
    )

    await run(arguments)

    value = json.loads(capsys.readouterr().out)
    assert value["request"]["loopCount"] == 3
