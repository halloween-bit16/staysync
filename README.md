# StaySync

StaySync is a desktop hotel management app for day-to-day front desk and admin operations.

## Non-Technical Features
- Manage room status (available, booked, cleaning, maintenance).
- Create and edit bookings with guest details and stay dates.
- Process checkout with payment tracking and bill generation.
- Manage discount/coupon codes (create, deactivate, reactivate).
- View guest history, audit logs, and basic revenue/operations dashboard.

## Technical Features
- JavaFX desktop UI with role-based navigation.
- SQLite database for core persistence.
- CSV exports/log files for operational records.
- Java 17 + Maven build pipeline.
- Thread-safe persistence methods using synchronized access in data layer.

## Demo Login Credentials
- **Admin**
  - Username: `admin`
  - Password: `admin123`
- **Reception**
  - Username: `reception`
  - Password: `hotel123`

## Run
- Build: `build.bat`
- Run: `run.bat`
