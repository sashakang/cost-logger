# User Manual

Welcome! This guide will help you set up and use the app. Don't worry - it's easier than it looks!

## Getting Started

Before you can use the app, you'll need to complete a few quick setup steps. This should only take a few minutes.

### Step 1: Enable Notification Access

First, we need permission to read notifications from your phone. Here's how:

1. Tap the "Open Settings" button when you see the notification access prompt
2. You'll be taken to your phone's settings
3. Find this app in the list and turn on the switch
4. Come back to the app

![Notification Settings](drawable:help_setup_notification_access)

*You'll see a screen like this - just turn on the switch next to the app name.*

**Important:** If you see a "Restricted setting" message that says the setting is unavailable, don't worry! This happens sometimes with debug builds. Here's how to fix it:

1. Go back to your phone's main Settings
2. Tap "Apps" or "Application manager"
3. Find "Notification Logger" in the list
4. Tap on it, then tap "Notification access" or "Special app access"
5. Turn on the switch there

That's it! The app can now read notifications from the apps you choose to track.

### Step 2: Sign In with Google

The app needs to connect to your Google account so it can save your notifications to a Google Sheet. Here's how to sign in:

1. Go to Settings (tap the gear icon in the top right)
2. Find the "Google Account" section
3. Tap "Sign in with Google"
4. Choose your Google account
5. Allow the app to access your Google Sheets

![Sign In Screen](drawable:help_setup_sign_in)

*You'll see a Google sign-in screen - just pick your account and allow access.*

Once you're signed in, you'll see your email address shown in the Settings screen.

### Step 3: Configure Your Google Sheet

Now you need to tell the app which Google Sheet to use. Don't worry if you don't have one yet - we'll help you create it!

**To find your Sheet ID:**

1. Open Google Sheets in your web browser
2. Create a new spreadsheet (or open an existing one)
3. Look at the web address (URL) at the top of your browser
4. You'll see something like: `docs.google.com/spreadsheets/d/ABC123XYZ456/edit`
5. Copy the part between `/d/` and `/edit` - that's your Sheet ID (ABC123XYZ456 in this example)

**To enter it in the app:**

1. Go to Settings
2. Find the "Google Sheet" section
3. Paste your Sheet ID in the "Sheet ID" field
4. (Optional) If your sheet has multiple tabs, enter the tab name in "Tab Name"

![Sheet Configuration](drawable:help_setup_sheet_config)

*This is where you paste your Sheet ID. The app will create the right columns automatically!*

**Tip:** If you leave "Tab Name" empty, the app will use the first tab in your spreadsheet.

### Step 4: Select Apps to Track

Now you get to choose which apps you want to track! The app will only log notifications from the apps you select.

1. Go to Settings
2. Tap "Select Apps to Track"
3. You'll see a list of all apps on your phone
4. Turn on the switch next to each app you want to track
5. Use the search bar at the top to find apps quickly

![App Selection](drawable:help_select_apps)

*Just turn on the switches for the apps you want to track. You can always change this later!*

**Tip:** You can search for apps by typing their name in the search box at the top.

## How to Use the App

### Automatic Notification Logging

Once you've completed the setup, the app works automatically! Here's what happens:

- When you get a notification from an app you're tracking, the app saves it
- Your notifications are saved to your Google Sheet automatically
- You don't need to do anything - it just works in the background

**How to check if it's working:**

- Go to Settings and look at "Pending Uploads" - this shows how many notifications are waiting to be saved
- If the number is 0, everything has been saved successfully!
- If you see a number, the app is working on uploading them (this happens automatically)

### Manual Transaction Entry

Sometimes you might want to enter a transaction manually (like cash payments). Here's how:

1. From the main screen, tap "Enter Transaction"
2. Choose an account (like "Cash" or one of your tracked apps)
3. Enter the amount (you can use negative amounts for expenses)
4. Pick a currency
5. Choose a category
6. Tap "Save"

![Transaction Entry](drawable:help_transaction_entry)

*Fill in all the fields and tap Save. The transaction will be added to your sheet!*

**Tip:** You can enter negative amounts (like -50.00) for expenses or refunds.

### Re-scanning Notifications

If you think the app might have missed some notifications, you can ask it to check again:

1. From the main screen, tap "Re-scan Notifications"
2. The app will check all current notifications from your tracked apps
3. You'll see a message telling you how many new notifications were found

**When to use this:**

- If you just enabled notification access and want to catch up on old notifications
- If you just added a new app to track and want to see its current notifications
- If you think something might have been missed

## Troubleshooting

### Notifications Not Being Logged

If notifications aren't being saved, check these things:

**1. Is notification access enabled?**
- Go to Settings and check the "Notification Access" status
- If it says "Not enabled", tap "Enable" and turn it on in your phone's settings

**2. Have you selected any apps to track?**
- Go to Settings → "Select Apps to Track"
- Make sure at least one app has its switch turned on
- If no apps are selected, the app won't log anything!

**3. Is the notification service running?**
- Try toggling notification access off and on again in your phone's settings
- This restarts the service and usually fixes connection issues

### Upload Failures

If you see "Pending Uploads" that won't go away:

**1. Check your internet connection**
- Make sure you're connected to Wi-Fi or mobile data
- The app needs internet to save to Google Sheets

**2. Check your Google sign-in**
- Go to Settings and make sure you're signed in
- If not, sign in again

**3. Check your Sheet ID**
- Make sure the Sheet ID in Settings is correct
- Try copying it again from your Google Sheet URL

**4. Try re-scanning**
- Sometimes a quick re-scan helps clear stuck uploads
- Tap "Re-scan Notifications" from the main screen

### Permission Issues

If you're having trouble with permissions:

**Notification Access:**
- Go to your phone's Settings → Apps → Special app access → Notification access
- Find this app and make sure it's turned on
- If it's already on, try turning it off and on again

**"Restricted setting" Message:**
If you see a message saying "For your security, this setting is currently unavailable":
- This is normal for debug builds or apps not from the Play Store
- Go to Settings → Apps → Notification Logger → Notification access
- Enable it from there instead
- The app will work the same way once enabled

**Google Account:**
- Make sure you're signed in to Google in the app
- If sign-in keeps failing, try signing out and signing back in
- Make sure you allow the app to access Google Sheets when prompted

**Still having problems?**
- Try closing the app completely and opening it again
- Make sure your phone's software is up to date
- Restart your phone if nothing else works


