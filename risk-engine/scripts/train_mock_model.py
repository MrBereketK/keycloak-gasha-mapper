import os
import joblib
import numpy as np
from sklearn.ensemble import IsolationForest


def generate_normal_login_data(n_samples=2000):
    """
    Generate realistic synthetic NORMAL login behavior.

    Features:
    [hour_sin, hour_cos, suspicious_ua, ip_reputation, privileged]
    """

    rng = np.random.default_rng(42)

    # Most normal users log in during daytime,
    # but some legitimate users work early/late.
    hour_weights = np.array([
        0.01, 0.005, 0.005, 0.005,
        0.01, 0.02, 0.04, 0.08,
        0.12, 0.12, 0.10, 0.09,
        0.08, 0.08, 0.07, 0.06,
        0.05, 0.04, 0.03, 0.02,
        0.015, 0.01, 0.01, 0.005
    ])
    
    # Normalize probabilities so they sum to exactly 1.0
    normalized_p = hour_weights / hour_weights.sum()

    hours = rng.choice(
        np.arange(24),
        size=n_samples,
        p=normalized_p
    )

    # Convert hour to cyclic representation.
    hour_sin = np.sin(2 * np.pi * hours / 24)
    hour_cos = np.cos(2 * np.pi * hours / 24)

    # Most normal logins use normal browsers.
    suspicious_ua = rng.choice(
        [0, 1],
        size=n_samples,
        p=[0.98, 0.02]
    )

    # Most normal IPs have good reputation.
    ip_reputation = rng.choice(
        [0, 1],
        size=n_samples,
        p=[0.97, 0.03]
    )

    # Some normal users are privileged.
    privileged = rng.choice(
        [0, 1],
        size=n_samples,
        p=[0.85, 0.15]
    )

    X = np.column_stack((
        hour_sin,
        hour_cos,
        suspicious_ua,
        ip_reputation,
        privileged
    ))

    return X


def train_and_save_model():

    print("Generating normal login training data...")

    # Isolation Forest should primarily learn NORMAL behavior.
    X_train = generate_normal_login_data(2000)

    print(f"Training samples: {len(X_train)}")
    print("Training Isolation Forest...")

    model = IsolationForest(
        contamination=0.05,
        n_estimators=200,
        random_state=42
    )

    model.fit(X_train)

    # Save model
    models_dir = os.path.join(
        os.path.dirname(__file__),
        "..",
        "app",
        "models"
    )

    os.makedirs(models_dir, exist_ok=True)

    model_path = os.path.join(
        models_dir,
        "risk_model_v2.pkl"
    )

    joblib.dump(model, model_path)

    print(f"Model successfully saved to: {model_path}")


if __name__ == "__main__":
    train_and_save_model()