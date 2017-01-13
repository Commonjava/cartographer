#!/usr/bin/python

import os
import requests
import argparse

BASE_URL = 'http://localhost:8082'

parser = argparse.ArgumentParser()
parser.add_argument('json_path', help='path for the request JSON file to send')
parser.add_argument('resource_path', help='operation path (under /api/depgraph) to use')
parser.add_argument('-A', '--accept', help='Accept header value to send (default: application/json)')
parser.add_argument('-C', '--content-type', help='Content-Type header value to send (default: application/json)')
parser.add_argument('-U', '--base-url', help="Base URL for Cartographer (NOT including '/api/*' path)")
args=parser.parse_args()

url = "%(base)s/api/depgraph/%(resource_path)s" % {
		'base': args.base_url if args.base_url else BASE_URL,
		'resource_path': args.resource_path
	}

with open(args.json_path) as f:
	request_json = f.read()

headers = {
	'Accept': args.accept if args.accept else 'application/json',
	'Content-Type': args.content_type if args.content_type else 'application/json'
}

response = requests.post(url, request_json, headers=headers)

if response.status_code == 200:
	print response.json()

else:
	raise Exception("Request failed with status: %s" % response.status_code)


