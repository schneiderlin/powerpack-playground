# GitHub Pages Deployment Setup

This workflow automatically deploys your static site from `target/powerpack/` to GitHub Pages.

## Deploy to Separate Public Repository

This keeps your main repository private while only the generated site is public.

### Steps:

1. **Create a new public repository** on GitHub (e.g., `your-username.github.io` or `your-username-pages`)

2. **Create a Personal Access Token (PAT)**:
   - Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Generate a new token with `repo` scope
   - Copy the token

3. **Add the token as a secret**:
   - In your **private repository** (this one), go to Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `PAGES_DEPLOY_TOKEN`
   - Value: Paste your PAT
   - Click "Add secret"

4. **Update the workflow file**:
   - Edit `.github/workflows/deploy.yml`
   - Uncomment and set the `external_repository` line:
     ```yaml
     external_repository: your-username/your-pages-repo
     publish_branch: main
     ```

5. **Configure GitHub Pages**:
   - Go to your **public repository** settings
   - Navigate to Pages section
   - Source: Deploy from a branch
   - Branch: `main` (or `gh-pages` if you used that)
   - Folder: `/ (root)`
   - Click Save

6. **Push to main branch** - The workflow will automatically deploy!

Your site will be available at: `https://your-username.github.io/your-pages-repo/`
