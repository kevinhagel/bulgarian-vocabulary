"""Configuration — loads from .env file."""
import os
from dotenv import load_dotenv

load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

BOT_TOKEN = os.environ["BOT_TOKEN"]
CHAT_ID = int(os.environ["CHAT_ID"])
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
BOT_API_KEY = os.environ["BOT_API_KEY"]
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")

# Lesson schedule: days are 0=Mon .. 6=Sun, time is HH:MM local
LESSON_DAYS = [1, 4]   # Tuesday, Friday
LESSON_TIME = "12:00"  # approximate — /lesson_today sends it manually

# Scheduled jobs
WORD_OF_DAY_TIME = "08:00"   # 24h local
SRS_NUDGE_TIME   = "18:00"   # 24h local
WEEKLY_SUMMARY_DAY = 6       # Sunday
WEEKLY_SUMMARY_TIME = "20:00"

# Quiet hours — no proactive alerts sent (0–6am)
QUIET_START = 0
QUIET_END   = 6
