<p align="center">
  <img src="../../assets/images/momently name.png" alt="Momently" width="260" />
</p>

<p align="center">Every moment counts</p>
<p align="center">Team Cybernauts · COS 301 · University of Pretoria · 2026</p>

<h1 align="center">Use Cases</h1>


## Use Case Summary

| ID | Title | Actor | Type |
|---|---|---|---|
| UC1 | Login | All Users | Base |
| UC2 | Register | All Users | Base |
| UC3 | Profile & Settings | All Users | Base |
| UC4 | Log & Manage Time Entries | Developer | Core |
| UC5 | View & Track Timesheets | Developer | Core |
| UC6 | Reports & Productivity | Developer | Core |
| UC7 | Submit Timesheet | Developer | Core |


## UC1: Login

**Actor:** All Users

**Goal:** Authenticate and gain access to the system.

**Precondition:** User has a registered account with a valid company email.

### Main Flow
1. User navigates to the login page.
2. User enters their company email and password.
3. System validates credentials against the registered account.
4. System generates a 6-digit OTP and sends it to the user's email.
5. User enters the OTP on the verification screen.
6. System validates the OTP and checks it has not expired (5 minute window).
7. System grants access and redirects to the dashboard.

### Acceptance Criteria
- User can log in successfully with valid company email, correct password, and valid OTP.
- After successful login, the user is redirected to their role-appropriate dashboard.
- "Remember Me" keeps the session active for 30 days on that device.
- Password reset link is sent to the registered email within 60 seconds.
- OTP is sent to the user's email within 30 seconds of successful credential validation.

### Negative Criteria
- Login is rejected if the email domain is not a recognised company domain.
- Login is rejected if the password is incorrect; the system shows a generic error without specifying which field is wrong.
- Login is blocked after 5 consecutive failed attempts; account is temporarily locked.
- An expired or already-used password reset link renders the reset form invalid.
- An incorrect OTP denies access; the user must retry or request a new OTP.
- An OTP older than 5 minutes is rejected as expired; the user must request a new one.

## UC2: Register

**Actor:** All Users

**Goal:** Create a new user account tied to a valid company email.

**Precondition:** User has a valid company email address. Registration page is publicly accessible.

### Main Flow
1. User navigates to the registration page.
2. User enters first name, last name, company email, and password.
3. System validates that the email belongs to an accepted company domain.
4. System creates the account and sends a verification email.
5. User clicks the verification link in their email.
6. Account is activated and user is redirected to the dashboard.

### Acceptance Criteria
- Account is created successfully when all required fields are valid and the email domain is accepted.
- A verification email is sent within 60 seconds of registration.
- Clicking the verification link activates the account and redirects the user directly to the dashboard.
- Passwords must meet the minimum strength requirements (at least 8 characters, one uppercase, one number, one special character).
- Newly registered users are assigned the Developer role by default until an admin changes it.
### Negative Criteria
- Registration is rejected if the email does not match an accepted company domain.
- Registration is rejected if any required field is empty.
- Registration is rejected if the email is already associated with an existing account.
- Passwords that do not meet the strength policy are rejected with a descriptive validation message.
- Clicking an expired verification link does not activate the account; the user must request a new link.


## UC3: Profile & Settings

**Actor:** All Users

**Goal:** View and update personal profile information and system preferences.

**Precondition:** User is authenticated.

### Main Flow
1. User navigates to their profile page via the top navigation or avatar menu.
2. System displays the current profile details and preferences.
3. User updates any editable fields and saves.
4. System validates and persists the changes.
5. System confirms with a success notification.

### Acceptance Criteria
- All profile fields (full name, job title, phone, email, department, location) can be edited and saved successfully.
- Avatar upload accepts JPG and PNG files under 5MB and displays the new image immediately.
- The light/dark mode toggle applies the theme instantly without a page reload.
- Theme preference persists across sessions: no need to re-select on next login.
- Log out clears the session and redirects to the login page within 1 second.
- Profile changes are reflected in the top navigation avatar and name display immediately.

### Negative Criteria
- Saving with an empty required field (full name, email) is rejected with a validation message.
- Uploading a file that is not JPG or PNG is rejected with a clear error.
- Uploading an image larger than 5MB is rejected.
- Changing the email to a non-company domain is rejected.
- The log out button invalidates the token server-side; using the old token after logout returns 401.


## UC4: Log & Manage Time Entries

**Actor:** Developer

**Goal:** Record time worked against projects and tasks, and manage those entries.

**Precondition:** User is authenticated as a Developer and has at least one project assigned.

### Main Flow
1. User navigates to the time logging page.
2. User clicks "Add Time Entry".
3. System presents the entry form.
4. User selects a project from the dropdown (populated with assigned projects only).
5. User selects a task from the dropdown (populated based on selected project).
6. User enters a start time and end time.
7. User enters an optional description.
8. User saves the entry.
9. System calculates duration from start/end time and saves the draft entry.
10. The entry appears in the current day's time log.

### Acceptance Criteria
- Project dropdown shows only projects the user is assigned to.
- Task dropdown is populated dynamically based on the selected project; it is empty and disabled until a project is chosen.
- Project, task, start time, and end time are required fields; description is optional.
- Duration is calculated server-side from start and end time; users cannot manually enter duration.
- The timer displays elapsed time in HH:MM:SS, updated every second.
- The timer can be paused and resumed without losing elapsed time.
- Only one active timer is allowed per user; starting a second is blocked.
- The time the timer was started is displayed while it is running.
- Stopping the timer automatically creates a draft entry without additional user action.
- Draft entries can be edited or deleted before submission.
- Saved entries appear immediately in the time log without a page reload.

### Negative Criteria
- A time entry with end time before or equal to start time is rejected.
- Saving without selecting a project or task is rejected with a validation message.
- Starting a second timer while one is already active is blocked; the system shows the active timer.
- Submitted or approved entries cannot be edited or deleted.
- Entries cannot be created for future dates.
- A time entry resulting in 0 minutes duration is rejected.
- Selecting a task that does not belong to the selected project is not possible.


## UC5: View & Track Timesheets

**Actor:** Developer

**Goal:** Review all time entries for the week, monitor hours progress, and track timesheet approval status.

**Precondition:** User is authenticated as a Developer.

### Main Flow
1. User navigates to the Timesheets page.
2. System displays the current week's time entries grouped by day.
3. System displays the weekly hours progress bar showing total logged hours out of the weekly target (default 40h, configurable).
4. System shows remaining hours needed to reach the target.
5. User can click on any entry to view its full detail.
6. User can view approval status of each timesheet entry.

### Acceptance Criteria
- The weekly progress bar accurately reflects the sum of all logged hours for the displayed week.
- Progress bar and remaining hours update immediately when a new entry is added or edited.
- The weekly target defaults to 40 hours and persists when changed by the user.
- Approval statuses are displayed with clear colour-coded chips: Draft, Submitted, Approved, Rejected.
- Rejected entries display the manager's rejection reason and feedback inline.
- Past weeks are accessible via week navigation controls.
- Filtering by project or status works without a full page reload.

### Negative Criteria
- Entries from other users are never visible on this page.
- If no entries exist for the selected week, a clear empty state is shown rather than a broken layout.
- Pending or rejected entries are not counted toward the weekly total in the progress bar.
- Navigating beyond account creation history shows an appropriate empty state.
- The weekly target cannot be set to zero or a negative number.


## UC6: Reports & Productivity

**Actor:** Developer

**Goal:** Generate and review productivity reports based on logged time entries within a selected date range.

**Precondition:** User is authenticated as a Developer and has at least one time entry logged.

### Main Flow
1. User navigates to the Reports page.
2. User selects a date range using the from/to date pickers.
3. System fetches all time entries within that period belonging to the user.
4. System displays the report with: total hours, time distribution by category, and productivity trend.

### Acceptance Criteria
- Report loads within 5 seconds for date ranges up to 12 months.
- Total hours correctly reflect the sum of all entries (approved and draft) in the selected range.
- Time distribution chart accurately reflects categories assigned to each entry.
- Productivity trend chart updates when the date range is changed.
- Quick-select options correctly set the date fields and trigger report generation.
- An empty state is shown with a clear message if no entries exist for the period.

### Negative Criteria
- A date range where end date is before start date is rejected with a validation error.
- Entries from other users are never included in the report.
- The report does not load without both a from and to date selected.
- Categories not assigned to any entry in the selected period are excluded from the distribution chart rather than shown as zero.


## UC7: Submit Timesheet

**Actor:** Developer

**Goal:** Submit the week's completed time entries for manager approval.

**Precondition:** User is authenticated as a Developer and has at least one draft entry for the week.

### Main Flow
1. User navigates to the Timesheets page.
2. User reviews the week's draft entries.
3. User clicks "Submit Timesheet".
4. System validates all entries are complete and the week has not already been submitted.
5. System changes all draft entries for that week to status: Submitted.
6. System locks the week from further editing.
7. System notifies the user's manager that a timesheet is pending review.
8. System confirms submission with a success message.

### Acceptance Criteria
- Clicking Submit changes all draft entries for that week to Submitted status simultaneously.
- The timesheet is locked from editing immediately after successful submission.
- The user's manager receives a notification upon submission.
- A success confirmation message is shown to the user.
- Rejected timesheets can be corrected and resubmitted; the status correctly returns to Submitted.
- Submission applies only to the selected week; other weeks are unaffected.

### Negative Criteria
- A week with no entries cannot be submitted; the Submit button is disabled or shows a blocking message.
- Entries with missing required fields block submission; the system highlights them.
- A week already in Submitted or Approved status cannot be resubmitted without a prior rejection.
- After submission, edit and delete controls are hidden or disabled for all entries in that week.
- Submitting for a past week that has already been approved does nothing and shows an informative message.
