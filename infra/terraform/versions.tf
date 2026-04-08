terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Month 1-2: add rg, aks, flexible server here; apply from CI or Azure Cloud Shell
# so your laptop never runs a real cluster locally
