"""SmartStock AI — application configuration.

All tunable constants live here. Engine and DB modules import from this module;
no magic numbers are scattered across the codebase.
"""

from __future__ import annotations

import os
from dataclasses import dataclass, field


@dataclass(frozen=True)
class Config:
    """Frozen configuration dataclass for SmartStock AI.

    All fields have sensible defaults; override via constructor kwargs or by
    setting the ``SMARTSTOCK_DB_PATH`` environment variable for the database path.
    """

    # --- Database ---
    db_path: str = field(
        default_factory=lambda: os.environ.get(
            "SMARTSTOCK_DB_PATH",
            os.path.normpath(
                os.path.join(os.path.dirname(__file__), "..", "data", "smartstock.db")
            ),
        )
    )
    db_migration_timeout_seconds: int = 30

    # --- Forecasting ---
    forecast_horizon_days: int = 30
    forecast_fallback_min_days: int = 30

    # --- Inventory policy ---
    service_level_z_score: float = 1.65
    ordering_cost: float = 25.0
    holding_cost_rate: float = 0.20
    dead_stock_threshold_days: int = 90

    # --- ML retraining ---
    retraining_min_new_records: int = 500

    # --- Allocation weights (must sum to 1.0 ± 0.001) ---
    alloc_weight_stock_health: float = 0.40
    alloc_weight_days_remaining: float = 0.35
    alloc_weight_revenue: float = 0.25

    def __post_init__(self) -> None:
        # Validate allocation weights
        total = (
            self.alloc_weight_stock_health
            + self.alloc_weight_days_remaining
            + self.alloc_weight_revenue
        )
        if abs(total - 1.0) > 0.001:
            raise ValueError(
                f"Allocation weights must sum to 1.0, got {total:.4f}"
            )

        # Validate db_path
        if not self.db_path or not isinstance(self.db_path, str):
            raise ValueError("db_path must be a non-empty string")
        if len(self.db_path) > 260:
            raise ValueError(
                f"db_path must be at most 260 characters, got {len(self.db_path)}"
            )

        # Validate retraining_min_new_records
        if not (1 <= self.retraining_min_new_records <= 1_000_000):
            raise ValueError(
                f"retraining_min_new_records must be between 1 and 1,000,000, "
                f"got {self.retraining_min_new_records}"
            )

        # Validate holding_cost_rate
        if not (0.01 <= self.holding_cost_rate <= 1.0):
            raise ValueError(
                f"holding_cost_rate must be between 0.01 and 1.0, "
                f"got {self.holding_cost_rate}"
            )


#: Module-level singleton using all default values.
DEFAULT_CONFIG = Config()
