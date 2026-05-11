UPDATE sign SET is_static = TRUE WHERE is_static IS DISTINCT FROM TRUE;
