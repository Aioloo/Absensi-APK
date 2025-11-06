"""
Custom password validators for TOTP authentication system.

Password requirements:
- Minimum 9 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 symbol (special character)
- No spaces allowed
"""

import re
from django.core.exceptions import ValidationError
from django.utils.translation import gettext as _


class CustomPasswordValidator:
    """
    Validates that the password meets all security requirements:
    - Minimum 9 characters
    - Contains at least 1 uppercase letter
    - Contains at least 1 lowercase letter
    - Contains at least 1 symbol (special character)
    - Contains no spaces
    """
    
    def validate(self, password, user=None):
        # Check minimum length
        if len(password) < 9:
            raise ValidationError(
                _("Password must be at least 9 characters long."),
                code='password_too_short',
            )
        
        # Check for spaces
        if ' ' in password:
            raise ValidationError(
                _("Password must not contain spaces."),
                code='password_contains_spaces',
            )
        
        # Check for uppercase letter
        if not re.search(r'[A-Z]', password):
            raise ValidationError(
                _("Password must contain at least 1 uppercase letter."),
                code='password_no_uppercase',
            )
        
        # Check for lowercase letter
        if not re.search(r'[a-z]', password):
            raise ValidationError(
                _("Password must contain at least 1 lowercase letter."),
                code='password_no_lowercase',
            )
        
        # Check for symbol (special character)
        if not re.search(r'[!@#$%^&*(),.?":{}|<>_\-+=\[\]\\\/;\'`~]', password):
            raise ValidationError(
                _("Password must contain at least 1 symbol (special character)."),
                code='password_no_symbol',
            )
    
    def get_help_text(self):
        return _(
            "Your password must be at least 9 characters long, "
            "contain at least 1 uppercase letter, 1 lowercase letter, "
            "1 symbol, and must not contain spaces."
        )
