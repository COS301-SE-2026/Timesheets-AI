# Shared Components Usage Guide (Momently UI Kit)

This document explains how to use the reusable UI components available in the `shared/components` library.

---

## 1. Button Component — `<app-button>`

### Purpose

Reusable button component with multiple styling variants.

### Basic Usage

```html
<app-button variant="primary">
  Save Changes
</app-button>
```

### Variants

```html
<app-button variant="primary">
  Primary
</app-button>

<app-button variant="secondary">
  Secondary
</app-button>

<app-button variant="tertiary">
  Tertiary
</app-button>
```

### With Icon

```html
<app-button variant="primary">
  <span>+</span> Add Item
</app-button>
```

---

## 2. Input Field — `<app-input-field>`

### Purpose

Reusable labelled input component with validation and error support.

### Basic Usage

```html
<app-input-field
  label="Project Name"
  placeholder="Enter project name">
</app-input-field>
```

### With Validation Error

```html
<app-input-field
  label="Hours"
  placeholder="Enter hours"
  [error]="'Invalid value'">
</app-input-field>
```

---

## 3. Dropdown — `<app-dropdown>`

### Purpose

Reusable select dropdown component with dynamic options.

### Basic Usage

```html
<app-dropdown
  [options]="['This Week', 'Last Week', 'This Month']">
</app-dropdown>
```

### Filter Example

```html
<app-dropdown
  [options]="['All', 'Active', 'Completed']">
</app-dropdown>
```

---

## 4. Loader — `<app-loader>`

### Purpose

Displays loading states during asynchronous operations.

### Basic Usage

```html
<app-loader></app-loader>
```

### Sizes

```html
<app-loader size="small"></app-loader>

<app-loader size="medium"></app-loader>

<app-loader size="large"></app-loader>
```

### Conditional Loading

```html
@if (loading) {
  <app-loader size="large"></app-loader>
}
```

---

## 5. Progress Bar — `<app-progress-bar>`

### Purpose

Displays completion progress visually.

### Basic Usage

```html
<app-progress-bar [progress]="75"></app-progress-bar>
```

### Example Inside a Card

```html
<div class="project-card">

  <h3>Website Tracker</h3>

  <app-progress-bar [progress]="40"></app-progress-bar>

</div>
```

---

## 6. Header — `<app-header>`

### Purpose

Reusable page header component with support for action content.

### Basic Usage

```html
<app-header title="Timesheets"></app-header>
```

### With Actions

```html
<app-header title="Projects">

  <app-button variant="primary">
    + New Project
  </app-button>

</app-header>
```

---

## 7. Sidebar — `<app-sidebar>`

### Purpose

Main router-driven navigation sidebar for the application layout.

### Usage

```html
<app-sidebar></app-sidebar>
```