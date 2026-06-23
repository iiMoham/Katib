# Katib website (`docs/`)

This folder is published as a static site via **GitHub Pages**. It serves:

- **Homepage:** `index.html` — the marketing landing page (also your "Website" URL for Play)
- **Privacy policy:** `privacy-policy.html` — the URL Google Play requires
- `.nojekyll` — tells GitHub Pages to serve files as-is (no Jekyll processing)

## Enable GitHub Pages (one time)

1. Push the repo to GitHub.
2. On GitHub: **Settings → Pages**.
3. Under **Build and deployment → Source**, choose **Deploy from a branch**.
4. Set **Branch = `main`** and **Folder = `/docs`**, then **Save**.
5. Wait ~1 minute. Your site goes live at:

```
Homepage : https://<your-username>.github.io/<repo-name>/
Privacy  : https://<your-username>.github.io/<repo-name>/privacy-policy.html
```

## Use these URLs

- Paste the **privacy URL** into Play Console → *App content → Privacy policy*
  (and update the link in the app: `SettingsScreen.kt` currently points to
  `https://katib.app/privacy`).
- Paste the **homepage URL** into the store listing's *Website* field.

## Custom domain (optional)

If you own `katib.app`, add a file named `CNAME` in this folder containing
`katib.app`, then configure the domain in **Settings → Pages → Custom domain**.
