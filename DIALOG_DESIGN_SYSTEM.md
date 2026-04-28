# Dialog Design System Guide

## Overview

The SariSariStore application now uses a comprehensive dialog design system to ensure consistency, accessibility, and professional appearance across all user interactions.

## Design System Foundation

### Base Unit: 8dp Grid
All spacing, padding, and margins are based on multiples of 8dp:
- `spacing_xs`: 4dp (half unit)
- `spacing_sm`: 8dp (1x)
- `spacing_md`: 16dp (2x)
- `spacing_lg`: 24dp (3x) - Primary dialog spacing
- `spacing_xl`: 32dp (4x) - Large content areas

### Typography Scale
```
Headline    → 24sp (text_size_heading)
Title       → 20sp (text_size_title) - Dialog headers
Subtitle    → 16sp (text_size_subtitle) - Section headers
Body Large  → 14sp (text_size_body_large) - Main text & buttons
Body Small  → 12sp (text_size_body_small) - Secondary text
Caption     → 11sp (text_size_caption) - Hints & labels
```

### Colors
**Primary Brand**: #D81B60 (Vibrant Pink)
**Text Colors**:
- Primary Text: #212121 (on light backgrounds)
- Secondary Text: #757575 (supporting information)
- Hint Text: #BDBDBD (disabled/placeholder)

**Semantic Colors**:
- Success: #4CAF50 (green)
- Error: #F44336 (red)
- Warning: #FFC107 (orange)
- Info: #2196F3 (blue)

## Dialog Component Template

### Standard Dialog Structure

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="@dimen/spacing_lg"
    app:cardCornerRadius="@dimen/dialog_corner_radius"
    app:cardElevation="8dp"
    app:cardBackgroundColor="@color/surface"
    app:strokeWidth="0dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- HEADER SECTION (Optional for simple dialogs) -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/primary"
            android:padding="@dimen/spacing_lg">
            
            <TextView
                android:text="Dialog Title"
                android:textSize="@dimen/text_size_title"
                android:textColor="@color/surface"
                android:textStyle="bold" />
        </LinearLayout>

        <!-- CONTENT SECTION -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingHorizontal="@dimen/spacing_lg"
            android:paddingVertical="@dimen/spacing_lg">
            
            <!-- Form fields go here with spacing_lg between them -->
        </LinearLayout>

        <!-- FOOTER SECTION (Action buttons) -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="end"
            android:paddingHorizontal="@dimen/spacing_md"
            android:paddingVertical="@dimen/spacing_sm">
            
            <!-- Buttons aligned to the right -->
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## Dialog Types and Usage

### 1. Success Dialog
**When to use**: Operation completed successfully

```java
DialogHelper.showSuccess(context, "Success", "Operation completed", callback);
DialogHelper.showSuccessAutoClose(context, "Success", "Message"); // Auto closes after 2 sec
```

**Layout**: `dialog_success.xml`
- Large success badge (72dp)
- Message with proper spacing
- "Great!" action button

### 2. Error Dialog
**When to use**: Operation failed or validation error

```java
DialogHelper.showError(context, "Error", "Error description", callback);
DialogHelper.showErrorAutoClose(context, "Error", "Quick error message"); // Auto closes after 3 sec
```

**Layout**: `dialog_error.xml`
- Large error badge (72dp) with red background
- Error message
- "Dismiss" button

### 3. Confirmation Dialog
**When to use**: Ask user to confirm an action

```java
DialogHelper.showConfirmation(context, 
    "Delete Item?", 
    "Are you sure?",
    () -> confirmAction(), 
    () -> cancelAction()
);
```

**Layout**: `dialog_confirmation.xml`
- Warning icon with orange background
- Title and message
- "Cancel" and "Confirm" buttons side-by-side

### 4. Loading Dialog
**When to use**: Indicate ongoing async operation

```java
AlertDialog dialog = DialogHelper.showLoading(context, "Loading products...");
// Later...
dialog.dismiss();
```

**Layout**: `dialog_loading.xml`
- Progress spinner
- Loading message
- Optional subtext

### 5. Form Input Dialog
**When to use**: Collect user input

**Layouts**:
- `dialog_add_product.xml` - Complex form with image, multiple fields
- `dialog_add_stock.xml` - Simple form for quantity
- `dialog_select_product.xml` - Quantity picker

**Pattern**:
```
Header (Primary Color) → Clear title
Content Area → Form fields, each with spacing_lg margin
Footer → Buttons aligned right
```

## Button Sizing Guide

| Type | Height | Width | Corner |
|------|--------|-------|--------|
| Primary Action | 48dp | match_parent | 16dp |
| Small Button | 44dp | wrap_content | 16dp |
| Text Button | wrap_content | wrap_content | 16dp |
| All buttons minimum tappable: 48dp |

## Form Field Spacing

```
┌─────────────────────────┐
│ Text Input 1            │  24dp margin bottom
│                         │
│ Text Input 2            │  24dp margin bottom
│                         │
│ Text Input 3            │  24dp margin bottom
│                         │
│ [Button] spacing: 8dp   │  24dp margin bottom
│                         │
└─────────────────────────┘
```

## Text Input Guidelines

All TextInputLayout fields should:
- Use `Material3.TextInputLayout.OutlinedBox` style
- Have `boxCornerRadius`: 16dp
- Have `boxStrokeColor`: primary color
- Children TextInputEditText should have:
  - `textColor`: text_primary
  - `textSize`: text_size_body_large (14sp)
  - `inputType`: appropriate for field

```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="@dimen/spacing_lg"
    android:hint="Field Label"
    app:boxCornerRadiusBottomEnd="@dimen/corner_md"
    app:boxCornerRadiusBottomStart="@dimen/corner_md"
    app:boxCornerRadiusTopEnd="@dimen/corner_md"
    app:boxCornerRadiusTopStart="@dimen/corner_md">
    
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/et_field_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:textColor="@color/text_primary"
        android:textSize="@dimen/text_size_body_large" />
</com.google.android.material.textfield.TextInputLayout>
```

## Color Usage in Dialogs

### Header Section
- `android:background="@color/primary"` (#D81B60)
- Text color: `@color/surface` (white)

### Icon Backgrounds
- **Success**: `@color/item_avatar_bg` (#E4F1AC)
- **Error**: `#FFEBEE` (light red)
- **Warning**: `#FFF3E0` (light orange)
- **Info**: `#E3F2FD` (light blue)

### Text on Headers
```xml
<TextView
    android:textColor="@color/surface"  <!-- White -->
    android:textStyle="bold"
    android:textSize="@dimen/text_size_title" />
```

## Creating New Dialogs - Step by Step

### Step 1: Create Layout File
```bash
# File: res/layout/dialog_my_action.xml
# Start with the template above
```

### Step 2: Add to DialogHelper (Optional)
```java
public static AlertDialog showMyDialog(Context context, String title, 
                                       String message, DialogCallback callback) {
    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(R.layout.dialog_my_action, null);
    
    // Setup views
    TextView tvTitle = view.findViewById(R.id.tv_title);
    Button btnAction = view.findViewById(R.id.btn_action);
    
    AlertDialog dialog = new MaterialAlertDialogBuilder(context, 
        R.style.Theme_SariSariStore_Dialog)
        .setView(view)
        .setCancelable(false)
        .create();
    
    btnAction.setOnClickListener(v -> {
        if (callback != null) callback.onAction();
        dialog.dismiss();
    });
    
    dialog.show();
    return dialog;
}
```

### Step 3: Use in Fragment
```java
DialogHelper.showMyDialog(requireContext(), "Title", "Message", () -> {
    // Action on positive button
});
```

## Best Practices

✅ **DO**:
- Use dimension references for all sizes
- Apply consistent 8dp grid spacing
- Use primary color for headers
- Auto-dismiss quick feedback dialogs
- Make buttons at least 48dp tall
- Use proper semantic colors

❌ **DON'T**:
- Hard-code dimension values
- Mix spacing units (mixing dp values)
- Use built-in dialogs inconsistently
- Create dialogs without headers
- Use inconsistent text sizes
- Forget to set corner radius

## Testing Your Dialog

1. **Layout Preview** → Check XML preview in Android Studio
2. **Size Testing** → Run on multiple device sizes
3. **Text Overflow** → Test with longer text
4. **Interaction** → Verify button clicks work
5. **Accessibility** → Check text contrast and size
6. **Orientation** → Test in landscape mode

## Troubleshooting

### Dialog appears cut off
- Check that margins aren't too large
- Use NestedScrollView for long content
- Verify dialog width is set to match_parent

### Text not visible
- Check colors have proper contrast
- Ensure text size uses dimension references
- Verify background color is set

### Buttons not clickable
- Check minimum height is 48dp
- Verify click listeners are attached
- Check for layout clipping

### Spacing looks inconsistent
- Use dimension references only
- Never hard-code spacing
- Follow the 8dp grid system
- Check all margin/padding attributes

## References

- Material Design 3: https://m3.material.io/
- Android Material Components: https://material.io/develop/android
- SariSariStore Design System: See dimens.xml and colors.xml

