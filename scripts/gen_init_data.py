#!/usr/bin/env python3
"""
Скрипт генерации валидного initData для локального тестирования Telegram Mini App в браузере.

Использование:
    python3 scripts/gen_init_data.py "$BOT_TOKEN" <telegram_id> [username] [first_name]

Пример:
    python3 scripts/gen_init_data.py "$BOT_TOKEN" 719564670
    Открыть в браузере: http://localhost:18081/?devInitData=<output>
"""

import sys
import time
import json
import hmac
import hashlib
import urllib.parse


def generate_init_data(bot_token: str, telegram_id: int, username: str = None, first_name: str = "Тестовый") -> str:
    # 1. user = json.dumps({...}, separators=(',', ':'), ensure_ascii=False) — RAW
    user_data = {"id": int(telegram_id)}
    if first_name:
        user_data["first_name"] = first_name
    if username:
        user_data["username"] = username
    user = json.dumps(user_data, separators=(",", ":"), ensure_ascii=False)

    # 2. params = {"auth_date": ..., "query_id": ..., "user": user} — RAW значения
    auth_date = str(int(time.time()))
    params = {
        "auth_date": auth_date,
        "query_id": "AAHdF6IQAAAAAN0XohDhrOrc",
        "user": user,
    }

    # 3. dataCheckString = "\n".join(f"{k}={v}" for k,v in sorted(params.items()))
    data_check_string = "\n".join(f"{k}={v}" for k, v in sorted(params.items()))

    # 4. hash = HMAC(secret, dataCheckString) — на RAW
    secret_key = hmac.new(b"WebAppData", bot_token.encode("utf-8"), hashlib.sha256).digest()
    calculated_hash = hmac.new(secret_key, data_check_string.encode("utf-8"), hashlib.sha256).hexdigest()

    # 5. init_data = urllib.parse.urlencode(params) — urlencode ОДИН раз
    params_with_hash = dict(params)
    params_with_hash["hash"] = calculated_hash
    init_data = urllib.parse.urlencode(params_with_hash)

    # 6. экранируем для ?devInitData=
    return urllib.parse.quote(init_data, safe="")


def main():
    if len(sys.argv) < 3:
        print("Использование: python3 scripts/gen_init_data.py <BOT_TOKEN> <telegram_id> [username] [first_name]", file=sys.stderr)
        sys.exit(1)

    bot_token = sys.argv[1].strip()
    try:
        telegram_id = int(sys.argv[2].strip())
    except ValueError:
        print("Ошибка: telegram_id должен быть числом", file=sys.stderr)
        sys.exit(1)

    username = sys.argv[3].strip() if len(sys.argv) > 3 else "test_user"
    first_name = sys.argv[4].strip() if len(sys.argv) > 4 else "Тестовый"

    result = generate_init_data(bot_token, telegram_id, username, first_name)
    print(result)


if __name__ == "__main__":
    main()
