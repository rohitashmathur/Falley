"""Synthetic exports only; never reads a player's Analytics data."""
import unittest
import importlib.util
from pathlib import Path
spec=importlib.util.spec_from_file_location("decoder",Path(__file__).with_name("decode-analytics-input.py"))
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
decode=module.decode


def event(stream, sample):
    p={"schema":1,"run_id":"abcdef-012345","stream":stream,"build_channel":"debug","sample_count":1,"s0":sample}
    return {"event_name":"falley_input_batch","user_pseudo_id":"never-export-this","event_params":[{"key":k,"value":{"string_value":v} if isinstance(v,str) else {"int_value":str(v)}} for k,v in p.items()]}

class DecoderTest(unittest.TestCase):
    def test_unfiltered_sensor_units(self):
        row=list(decode(event("accel","500,1.234567,9.81,-2.5,1")))[0]
        self.assertEqual(row["x"],1.234567)
        self.assertEqual(row["rotation"],1)
        self.assertNotIn("never-export-this",str(row))
    def test_touch(self):
        row=list(decode(event("touch","40,2,123.5,456.25,720,1600")))[0]
        self.assertEqual((row["phase"],row["width"],row["height"]),(2,720,1600))
    def test_bad_schema_and_nonfinite(self):
        for value in ["500,NaN,0,0,1","500,0,0,0,9","broken"]:
            with self.assertRaises(ValueError):list(decode(event("gyro",value)))
    def test_unrelated_event(self):
        self.assertEqual(list(decode({"event_name":"session_start"})),[])

if __name__=="__main__":unittest.main()
