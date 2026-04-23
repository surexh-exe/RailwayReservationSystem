# Railway Management System

A desktop-based Railway Reservation System built with Java Swing that simulates core railway booking workflows.

## Overview

This project provides an interactive GUI for searching trains, booking tickets, viewing booking history, and managing fare/capacity configurations through an admin panel.

It is designed as an academic mini-project to demonstrate:
- Java Swing GUI development
- Graph-based route distance calculation
- File handling and data persistence
- Basic reservation and cancellation logic

## Features

- Login screen with simple admin authentication (`admin/admin`)
- Train search by source, destination, and journey date
- Route-aware distance calculation using graph traversal
- Fare calculation by class and distance
- Passenger booking with:
  - Auto-generated PNR
  - Seat preference (Window, Middle, Aisle)
  - Seat allocation by class and coach
- Coach-wise seat map visualization (available vs booked)
- Booking confirmation view with complete ticket details
- Booking history table
- Cancel booking by PNR with rule-based refund calculation
- Persistent booking storage in `bookings.txt`
- Admin panel to override:
  - Class-wise seat capacity
  - Class-wise fare rate per km

## Tech Stack

- Language: Java
- UI: Java Swing
- Storage: Plain text file (`bookings.txt`)

## Project Structure

- `Main.java` - Complete application source code (UI + business logic)
- `bookings.txt` - Persisted booking records

## How to Run

### 1. Compile

```bash
javac Main.java
```

### 2. Run

```bash
java Main
```

## Usage Flow

1. Log in using username: `admin` and password: `admin`
2. Search trains with source, destination, and date
3. Select a train and proceed to booking
4. Enter passenger and payment details
5. Confirm booking and note the generated PNR
6. Use menu options to:
   - View booking history
   - Cancel booking by PNR
   - Open admin panel

## Cancellation Rules

- `>= 7 days` before journey: 10% charge
- `2 to 6 days` before journey: 25% charge
- `0 to 1 day` before journey: 50% charge
- After journey date: 100% charge

## Learning Highlights

- Shortest path style distance calculation between stations
- Dynamic fare and seat inventory updates
- Swing table/forms/dialog-based UI design
- File-based persistence and backward-compatible booking loading

## Future Improvements

- Store data in a database (MySQL/SQLite)
- Add role-based authentication
- Improve seat allocation strategy with quotas/waitlist
- Export tickets as PDF
- Add unit tests for fare and cancellation modules


